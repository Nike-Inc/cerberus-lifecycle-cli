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

package com.nike.cerberus.command.core;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.LaunchConfigParameters;
import com.nike.cerberus.domain.cloudformation.SslConfigParametersDelegate;
import com.nike.cerberus.domain.cloudformation.TagParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.command.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.AmiTagCheckService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;
import static com.nike.cerberus.ConfigConstants.CERT_PART_PUBKEY;
import static com.nike.cerberus.command.core.UpdateStackCommand.COMMAND_NAME;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_ARG;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_DESCRIPTION;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Command for updating the specified CloudFormation stack with the new parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the specified CloudFormation stack.")
public class UpdateStackCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "update-stack";

    public static final String OVERWRITE_TEMPLATE_LONG_ARG = "--overwrite-template";

    public static final String PARAMETER_SHORT_ARG = "-P";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to update.")
    private StackName stackName;

    @Parameter(names = StackDelegate.OWNER_GROUP_LONG_ARG,
            description = "The owning group for the resources to be updated. Will be tagged on all resources.",
            required = true)
    private String ownerGroup;

    @Parameter(names = StackDelegate.AMI_ID_LONG_ARG, description = "The AMI ID for the specified stack.")
    private String amiId;

    @Parameter(names = StackDelegate.INSTANCE_SIZE_LONG_ARG, description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = StackDelegate.KEY_PAIR_NAME_LONG_ARG, description = "SSH key pair name.")
    private String keyPairName;

    @Parameter(names = StackDelegate.OWNER_EMAIL_LONG_ARG,
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.")
    private String ownerEmail;

    @Parameter(names = StackDelegate.COST_CENTER_LONG_ARG,
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.")
    private String costcenter;

    @Parameter(names = OVERWRITE_TEMPLATE_LONG_ARG,
            description = "Flag for overwriting existing CloudFormation template")
    private boolean overwriteTemplate;

    @Parameter(names = SKIP_AMI_TAG_CHECK_ARG,
            description = SKIP_AMI_TAG_CHECK_DESCRIPTION)
    private boolean skipAmiTagCheck;

    @DynamicParameter(names = PARAMETER_SHORT_ARG, description = "Dynamic parameters for overriding the values for specific parameters in the CloudFormation.")
    private Map<String, String> dynamicParameters = new HashMap<>();

    private final Map<StackName, Class<? extends LaunchConfigParameters>> stackParameterMap;

    private final Map<StackName, String> stackTemplatePathMap;

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper cloudFormationObjectMapper;

    private final Ec2UserDataService ec2UserDataService;

    private final AmiTagCheckService amiTagCheckService;

    @Inject
    public UpdateStackCommand(final ConfigStore configStore,
                                final CloudFormationService cloudFormationService,
                                @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudFormationObjectMapper,
                                final Ec2UserDataService ec2UserDataService,
                                final AmiTagCheckService amiTagCheckService) {
        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.ec2UserDataService = ec2UserDataService;
        this.amiTagCheckService = amiTagCheckService;

        stackParameterMap = new HashMap<>();
        stackParameterMap.put(StackName.CMS, CmsParameters.class);

        stackTemplatePathMap = new HashMap<>();
        stackTemplatePathMap.put(StackName.BASE, ConfigConstants.VPC_STACK_TEMPLATE_PATH);
        stackTemplatePathMap.put(StackName.CMS, ConfigConstants.CMS_STACK_TEMPLATE_PATH);
    }

    @Override
    public void execute() {
        final String stackId = configStore.getStackId(stackName);
        final Class<? extends LaunchConfigParameters> parametersClass = stackParameterMap.get(stackName);
        final Map<String, String> parameters;

        if (parametersClass != null) {
            parameters = getUpdateLaunchConfigParameters(stackName, parametersClass);
        } else if (StackName.BASE == stackName) {
            parameters = getUpdatedBaseStackParameters();
        } else {
            throw new IllegalArgumentException("The specified stack does not support the update stack command!");
        }

        // Make sure the given AmiId is for this component. Check if it contains required tag
        // There is no AMI for Base.
        if ( ! skipAmiTagCheck && StackName.BASE != stackName ) {
            amiTagCheckService.validateAmiTagForStack(amiId,stackName);
        }

        parameters.putAll(dynamicParameters);

        try {
            logger.info("Starting the update for {}.", stackName.getName());

            if (overwriteTemplate) {
                cloudFormationService.updateStack(stackId, parameters, stackTemplatePathMap.get(stackName), true);
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
    public boolean isRunnable() {
        boolean isRunnable = true;
        final String stackId = configStore.getStackId(stackName);

        if (StringUtils.isBlank(stackId)) {
            logger.error("The stack name specified has not been created for this environment yet!");
            isRunnable = false;
        } else if (!cloudFormationService.isStackPresent(stackId)) {
            logger.error("CloudFormation doesn't have the specified stack: {}", stackId);
            isRunnable = false;
        }

        return isRunnable;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    private Map<String, String> getUpdateLaunchConfigParameters(final StackName stackName,
                                                                final Class<? extends LaunchConfigParameters> parametersClass) {
        final LaunchConfigParameters launchConfigParameters =
                configStore.getStackParameters(stackName, parametersClass);

        launchConfigParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(stackName, ownerGroup));

        if (StringUtils.isNotBlank(amiId)) {
            launchConfigParameters.getLaunchConfigParameters().setAmiId(amiId);
        }

        if (StringUtils.isNotBlank(instanceSize)) {
            launchConfigParameters.getLaunchConfigParameters().setInstanceSize(instanceSize);
        }

        if (StringUtils.isNotBlank(keyPairName)) {
            launchConfigParameters.getLaunchConfigParameters().setKeyPairName(keyPairName);
        }

        if (StringUtils.isNotBlank(ownerEmail)) {
            launchConfigParameters.getTagParameters().setTagEmail(ownerEmail);
        }

        if (StringUtils.isNotBlank(costcenter)) {
            launchConfigParameters.getTagParameters().setTagCostcenter(costcenter);
        }

        updateSslConfigParameters(stackName, launchConfigParameters);

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        return cloudFormationObjectMapper.convertValue(launchConfigParameters, typeReference);
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

    private Map<String, String> getUpdatedBaseStackParameters() {
        final TagParameters tagParameters = configStore.getStackParameters(stackName, BaseParameters.class);

        if (StringUtils.isNotBlank(ownerEmail)) {
            tagParameters.getTagParameters().setTagEmail(ownerEmail);
        }

        if (StringUtils.isNotBlank(costcenter)) {
            tagParameters.getTagParameters().setTagCostcenter(costcenter);
        }

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        return cloudFormationObjectMapper.convertValue(tagParameters, typeReference);
    }
}
