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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.GatewayParameters;
import com.nike.cerberus.domain.cloudformation.LaunchConfigParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
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
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation for updating stacks.
 */
public class UpdateStackOperation implements Operation<UpdateStackCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Stack, Class<? extends LaunchConfigParameters>> stackParameterMap;

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
        stackParameterMap.put(Stack.CMS, CmsParameters.class);
        stackParameterMap.put(Stack.GATEWAY, GatewayParameters.class);

    }

    @Override
    public void run(final UpdateStackCommand command) {
        final String stackId = configStore.getStackId(command.getStack());
        final Class<? extends LaunchConfigParameters> parametersClass = stackParameterMap.get(command.getStack());
        final Map<String, String> parameters;

        if (parametersClass != null) {
            parameters = getUpdateLaunchConfigParameters(command.getStack(), command, parametersClass);
        } else if (Stack.BASE == command.getStack()) {
            parameters = getUpdatedBaseStackParameters(command);
        } else {
            throw new IllegalArgumentException("The specified stack does not support the update stack command!");
        }

        // Make sure the given AmiId is for this component. Check if it contains required tag
        // There is no AMI for Base.
        if (!command.isSkipAmiTagCheck() && Stack.BASE != command.getStack()) {
            amiTagCheckService.validateAmiTagForStack(command.getStackDelegate().getAmiId(), command.getStack());
        }

        parameters.putAll(command.getDynamicParameters());

        try {
            logger.info("Starting the update for {}.", command.getStack().getName());

            cloudFormationService.updateStack(command.getStack(), parameters,
                    true, command.isOverwriteTemplate(), command.getStackDelegate().getTagParameters().getTags());

            final StackStatus endStatus =
                    cloudFormationService.waitForStatus(stackId,
                            Sets.newHashSet(
                                    UPDATE_COMPLETE,
                                    UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_COMPLETE,
                                    UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_FAILED
                            ));

            if (!ImmutableList.of(UPDATE_COMPLETE, UPDATE_COMPLETE_CLEANUP_IN_PROGRESS).contains(endStatus)) {
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
        final String stackId = configStore.getStackId(command.getStack());

        if (StringUtils.isBlank(stackId)) {
            logger.error("The stack name specified has not been created for this environment yet!");
            isRunnable = false;
        } else if (!cloudFormationService.isStackPresent(stackId)) {
            logger.error("CloudFormation doesn't have the specified stack: {}", stackId);
            isRunnable = false;
        }

        return isRunnable;
    }

    private Map<String, String> getUpdateLaunchConfigParameters(final Stack stack,
                                                                final UpdateStackCommand command,
                                                                final Class<? extends LaunchConfigParameters> parametersClass) {
        final LaunchConfigParameters launchConfigParameters =
                configStore.getStackParameters(stack, parametersClass);

        launchConfigParameters.getLaunchConfigParameters().setUserData(ec2UserDataService.getUserData(stack));

        if (StringUtils.isNotBlank(command.getStackDelegate().getAmiId())) {
            launchConfigParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        }

        if (StringUtils.isNotBlank(command.getStackDelegate().getInstanceSize())) {
            launchConfigParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        }

        if (StringUtils.isNotBlank(command.getStackDelegate().getKeyPairName())) {
            launchConfigParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        }
        
        return cloudformationObjectMapper.convertValue(launchConfigParameters, new TypeReference<Map<String, String>>() {});
    }

    private Map<String, String> getUpdatedBaseStackParameters(final UpdateStackCommand command) {
        return Maps.newHashMap();
    }
}