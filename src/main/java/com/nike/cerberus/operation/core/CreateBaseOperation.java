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

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.MapOfStringsTypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateBaseOperation implements Operation<CreateBaseCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateBaseOperation(final EnvironmentMetadata environmentMetadata,
                               final CloudFormationService cloudFormationService,
                               final ConfigStore configStore,
                               @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateBaseCommand command) {
        final BaseParameters baseParameters = new BaseParameters()
                .setAccountAdminArn(command.getAdminRoleArn());

        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(baseParameters, new MapOfStringsTypeRef());

        final String stackId = cloudFormationService.createStack(Stack.BASE, parameters, true,
                command.getTagsDelegate().getTags());

        final StackStatus endStatus = cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (StackStatus.CREATE_COMPLETE == endStatus) {
            logger.info("Stack creation complete, initializing the configuration bucket.");
            environmentMetadata.setBucketName(configStore.getBaseStackOutputs().getConfigBucketName());
            configStore.initEnvironmentData();
            configStore.initSecretsData();
            logger.info("Initialization complete.");
        } else {
            throw new UnexpectedCloudFormationStatusException(String.format("Unexpected end status: %s", endStatus.name()));
        }
    }

    @Override
    public boolean isRunnable(final CreateBaseCommand command) {
        String environmentName = environmentMetadata.getName();
        return !cloudFormationService.isStackPresent(Stack.BASE.getFullName(environmentName));
    }

}
