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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.CreateDatabaseCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.DatabaseParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.nike.cerberus.ConfigConstants.MINIMUM_AZS;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateDatabaseOperation implements Operation<CreateDatabaseCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    private RandomStringGenerator passwordGenerator = new RandomStringGenerator();

    @Inject
    public CreateDatabaseOperation(final EnvironmentMetadata environmentMetadata,
                                       final CloudFormationService cloudFormationService,
                                       final ConfigStore configStore,
                                       @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateDatabaseCommand command) {
        final String environmentName = environmentMetadata.getName();
        final VpcParameters vpcParameters = configStore.getVpcStackParameters();
        final VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();

        final DatabaseParameters databaseParameters = new DatabaseParameters()
                .setCmsDbMasterPassword(passwordGenerator.get())
                .setSgStackName(StackName.SECURITY_GROUPS.getFullName(environmentName))
                .setCmsDbInstanceAz1(vpcParameters.getAz1())
                .setCmsDbInstanceAz2(vpcParameters.getAz2())
                .setCmsDbInstanceAz3(vpcParameters.getAz3())
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3());

        databaseParameters.getTagParameters().setTagEmail(command.getTagsDelegate().getTagEmail());
        databaseParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentName);
        databaseParameters.getTagParameters().setTagCostcenter(command.getTagsDelegate().getTagCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(databaseParameters, typeReference);

        cloudFormationService.createStack(StackName.DATABASE.getFullName(environmentName),
                parameters, ConfigConstants.DATABASE_STACK_TEMPLATE_PATH, true);    }

    @Override
    public boolean isRunnable(final CreateDatabaseCommand command) {
        String environmentName = environmentMetadata.getName();
        try {
            cloudFormationService.getStackId(StackName.SECURITY_GROUPS.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("The load balancer stack must exist to create the Route53 record!", iae);
        }

        return ! cloudFormationService.isStackPresent(StackName.DATABASE.getFullName(environmentName));
    }
}
