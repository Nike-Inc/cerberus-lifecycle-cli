/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.operation.core;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.ConsulParameters;
import com.nike.cerberus.domain.cloudformation.GatewayParameters;
import com.nike.cerberus.domain.cloudformation.LaunchConfigParameters;
import com.nike.cerberus.domain.cloudformation.SslConfigParametersDelegate;
import com.nike.cerberus.domain.cloudformation.TagParameters;
import com.nike.cerberus.domain.cloudformation.VaultParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.service.AmiTagCheckService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.cloudformation.model.StackStatus.*;
import static com.nike.cerberus.ConfigConstants.CERT_PART_PUBKEY;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation for updating stacks.
 */
public class UpdateStackOperation implements Operation<UpdateStackCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<StackName, Class<? extends LaunchConfigParameters>> stackParameterMap;

    private final Map<StackName, String> stackTemplatePathMap;

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper cloudformationObjectMapper;

    private final Ec2UserDataService ec2UserDataService;

    private final AmiTagCheckService amiTagCheckService;

    @Inject
    public UpdateStackOperation(final ConfigStore configStore,
                                final CloudFormationService cloudFormationService,
                                @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper,
                                final Ec2UserDataService ec2UserDataService,
                                final AmiTagCheckService amiTagCheckService) {
        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
        this.ec2UserDataService = ec2UserDataService;
        this.amiTagCheckService = amiTagCheckService;

        stackParameterMap = new HashMap<>();
        stackParameterMap.put(StackName.CONSUL, ConsulParameters.class);
        stackParameterMap.put(StackName.VAULT, VaultParameters.class);
        stackParameterMap.put(StackName.CMS, CmsParameters.class);
        stackParameterMap.put(StackName.GATEWAY, GatewayParameters.class);

        stackTemplatePathMap = new HashMap<>();
        stackTemplatePathMap.put(StackName.BASE, ConfigConstants.BASE_STACK_TEMPLATE_PATH);
        stackTemplatePathMap.put(StackName.CONSUL, ConfigConstants.CONSUL_STACK_TEMPLATE_PATH);
        stackTemplatePathMap.put(StackName.VAULT, ConfigConstants.VAULT_STACK_TEMPLATE_PATH);
        stackTemplatePathMap.put(StackName.CMS, ConfigConstants.CMS_STACK_TEMPLATE_PATH);
        stackTemplatePathMap.put(StackName.GATEWAY, ConfigConstants.GATEWAY_STACK_TEMPLATE_PATH);
    }

    @Override
    public void run(final UpdateStackCommand command) {
        final String stackId = configStore.getStackId(command.getStackName());
        final Class<? extends LaunchConfigParameters> parametersClass = stackParameterMap.get(command.getStackName());
        final Map<String, String> parameters;

        if (parametersClass != null) {
            parameters = getUpdateLaunchConfigParameters(command.getStackName(), command, parametersClass);
        } else if (StackName.BASE == command.getStackName()) {
            parameters = getUpdatedBaseStackParameters(command);
        } else {
            throw new IllegalArgumentException("The specified stack does not support the update stack command!");
        }

        // Make sure the given AmiId is for this component. Check if it contains required tag
        // There is no AMI for Base.
        if ( !command.isSkipAmiTagCheck() && StackName.BASE != command.getStackName() ) {
            amiTagCheckService.validateAmiTagForStack(command.getAmiId(),command.getStackName());
        }

        parameters.putAll(command.getDynamicParameters());

        try {
            logger.info("Starting the update for {}.", command.getStackName().getName());

            if (command.isOverwriteTemplate()) {
                cloudFormationService.updateStack(stackId, parameters, stackTemplatePathMap.get(command.getStackName()), true);
            } else {
                cloudFormationService.updateStack(stackId, parameters, true);
            }

            final StackStatus endStatus =
                    cloudFormationService.waitForStatus(stackId,
                            Sets.newHashSet(
                                    UPDATE_COMPLETE,
                                    UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_COMPLETE,
                                    UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_FAILED
                            ));

            if (! ImmutableList.of(UPDATE_COMPLETE, UPDATE_COMPLETE_CLEANUP_IN_PROGRESS).contains(endStatus)) {
                final String errorMessage = String.format("CloudFormation reports that updating the stack was not successful. end status: %s", endStatus.name());
                logger.error(errorMessage);

                throw new UnexpectedCloudFormationStatusException(errorMessage);
            }

            logger.info("Update complete.");
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == 400 &&
                    StringUtils.equalsIgnoreCase(ase.getErrorMessage(), "No updates are to be performed.")) {
                logger.warn("CloudFormation reported no changes detected.");
            } else {
                throw ase;
            }
        }
    }

    @Override
    public boolean isRunnable(final UpdateStackCommand command) {
        boolean isRunnable = true;
        final String stackId = configStore.getStackId(command.getStackName());

        if (StringUtils.isBlank(stackId)) {
            logger.error("The stack name specified has not been created for this environment yet!");
            isRunnable = false;
        } else if (!cloudFormationService.isStackPresent(stackId)) {
            logger.error("CloudFormation doesn't have the specified stack: {}", stackId);
            isRunnable = false;
        }

        return isRunnable;
    }

    private Map<String, String> getUpdateLaunchConfigParameters(final StackName stackName,
                                                                final UpdateStackCommand command,
                                                                final Class<? extends LaunchConfigParameters> parametersClass) {
        final LaunchConfigParameters launchConfigParameters =
                configStore.getStackParameters(stackName, parametersClass);

        launchConfigParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(stackName, command.getOwnerGroup()));

        if (StringUtils.isNotBlank(command.getAmiId())) {
            launchConfigParameters.getLaunchConfigParameters().setAmiId(command.getAmiId());
        }

        if (StringUtils.isNotBlank(command.getInstanceSize())) {
            launchConfigParameters.getLaunchConfigParameters().setInstanceSize(command.getInstanceSize());
        }

        if (StringUtils.isNotBlank(command.getKeyPairName())) {
            launchConfigParameters.getLaunchConfigParameters().setKeyPairName(command.getKeyPairName());
        }

        if (StringUtils.isNotBlank(command.getOwnerEmail())) {
            launchConfigParameters.getTagParameters().setTagEmail(command.getOwnerEmail());
        }

        if (StringUtils.isNotBlank(command.getCostcenter())) {
            launchConfigParameters.getTagParameters().setTagCostcenter(command.getCostcenter());
        }

        if (command.getDesiredInstances() != null) {
            launchConfigParameters.getLaunchConfigParameters().setDesiredInstances(command.getDesiredInstances());
        }

        if (command.getMinimumInstances() != null) {
            launchConfigParameters.getLaunchConfigParameters().setMinimumInstances(command.getMinimumInstances());
        }

        if (command.getMaximumInstances() != null) {
            launchConfigParameters.getLaunchConfigParameters().setMaximumInstances(command.getMaximumInstances());
        }

        updateSslConfigParameters(stackName, launchConfigParameters);

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        return cloudformationObjectMapper.convertValue(launchConfigParameters, typeReference);
    }

    private void updateSslConfigParameters(final StackName stackName,
                                           final LaunchConfigParameters launchConfigParameters) {

        final SslConfigParametersDelegate sslConfigParameters = launchConfigParameters.getSslConfigParameters();

        if (StringUtils.isNotBlank(sslConfigParameters.getCertPublicKey())) {
            sslConfigParameters.setCertPublicKey(configStore.getCertPart(stackName, CERT_PART_PUBKEY).get());
        }

        if (StringUtils.isNotBlank(sslConfigParameters.getSslCertificateId())) {
            sslConfigParameters.setSslCertificateId(configStore.getServerCertificateId(stackName).get());
        }

        if (StringUtils.isNotBlank(sslConfigParameters.getSslCertificateArn())) {
            sslConfigParameters.setSslCertificateArn(configStore.getServerCertificateArn(stackName).get());
        }
    }

    private Map<String, String> getUpdatedBaseStackParameters(final UpdateStackCommand command) {
        final TagParameters tagParameters = configStore.getStackParameters(command.getStackName(), BaseParameters.class);

        if (StringUtils.isNotBlank(command.getOwnerEmail())) {
            tagParameters.getTagParameters().setTagEmail(command.getOwnerEmail());
        }

        if (StringUtils.isNotBlank(command.getCostcenter())) {
            tagParameters.getTagParameters().setTagCostcenter(command.getCostcenter());
        }

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        return cloudformationObjectMapper.convertValue(tagParameters, typeReference);
    }
}
