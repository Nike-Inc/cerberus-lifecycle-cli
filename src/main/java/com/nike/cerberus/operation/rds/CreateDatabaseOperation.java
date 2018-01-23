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

package com.nike.cerberus.operation.rds;

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.rds.CreateDatabaseCommand;
import com.nike.cerberus.domain.cloudformation.DatabaseParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import com.nike.cerberus.util.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateDatabaseOperation implements Operation<CreateDatabaseCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private RandomStringGenerator passwordGenerator = new RandomStringGenerator();

    @Inject
    public CreateDatabaseOperation(@Named(ENV_NAME) String environmentName,
                                   CloudFormationService cloudFormationService,
                                   ConfigStore configStore,
                                   CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(CreateDatabaseCommand command) {
        Regions region = configStore.getPrimaryRegion();

        VpcParameters vpcParameters = configStore.getVpcStackParameters();
        VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();
        String databasePassword = passwordGenerator.get();

        DatabaseParameters databaseParameters = new DatabaseParameters()
                .setCmsDbMasterPassword(databasePassword)
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName))
                .setCmsDbInstanceAz1(vpcParameters.getAz1())
                .setCmsDbInstanceAz2(vpcParameters.getAz2())
                .setCmsDbInstanceAz3(vpcParameters.getAz3())
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3())
                .setCmsDbInstanceClass(command.getInstanceClass())
                .setSnapshotIdentifier(command.getSnapshotIdentifier())
                .setVpcInternalBaseDomainName(vpcOutputs.getVpcInternalBaseDomainName())
                .setVpcInternalHostedZoneId(vpcOutputs.getVpcInternalHostedZoneId());

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(databaseParameters);

        configStore.storeCmsDatabasePassword(databasePassword);

        cloudFormationService.createStackAndWait(
                region,
                Stack.DATABASE,
                parameters,
                true,
                command.getTagsDelegate().getTags()
        );
    }

    @Override
    public boolean isRunnable(CreateDatabaseCommand command) {
        boolean isRunnable = true;

        if (!cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.SECURITY_GROUPS.getFullName(environmentName))) {
            isRunnable = false;
            logger.error("The security group stack must exist to create the the data base stack!");
        }

        return isRunnable;
    }
}
