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

import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.domain.cloudformation.SecurityGroupParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateSecurityGroupsOperation implements Operation<CreateSecurityGroupsCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateSecurityGroupsOperation(@Named(ENV_NAME) String environmentName,
                                         CloudFormationService cloudFormationService,
                                         ConfigStore configStore,
                                         CloudFormationObjectMapper cloudFormationObjectMapper) {
        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(CreateSecurityGroupsCommand command) {
        VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();

        SecurityGroupParameters securityGroupParameters = new SecurityGroupParameters()
                .setVpcId(vpcOutputs.getVpcId());

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(securityGroupParameters);

        cloudFormationService.createStackAndWait(
                configStore.getPrimaryRegion(),
                Stack.SECURITY_GROUPS,
                parameters,
                true,
                command.getCloudFormationParametersDelegate().getTags());
    }

    @Override
    public boolean isRunnable(CreateSecurityGroupsCommand command) {
        return !cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.SECURITY_GROUPS.getFullName(environmentName));
    }

}
