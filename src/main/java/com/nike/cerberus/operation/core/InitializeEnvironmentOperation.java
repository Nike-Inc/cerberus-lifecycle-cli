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

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.core.InitializeEnvironmentCommand;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class InitializeEnvironmentOperation implements Operation<InitializeEnvironmentCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    @Inject
    public InitializeEnvironmentOperation(@Named(ENV_NAME) String environmentName,
                                          CloudFormationService cloudFormationService,
                                          ConfigStore configStore,
                                          CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(InitializeEnvironmentCommand command) {
        Regions primaryRegion = Regions.fromName(command.getPrimaryRegion());

        // create the global cms iam role
        cloudFormationService.createStackAndWait(primaryRegion, Stack.CMS_IAM_ROLE, new HashMap<>(), true,
                command.getTagsDelegate().getTags());

        String cmsIamRoleArn = configStore.getCmsIamRoleOutputs(primaryRegion).getCmsIamRoleArn();

        BaseParameters baseParameters = new BaseParameters()
                .setAccountAdminArn(command.getAdminRoleArn())
                .setCmsIamRoleArn(cmsIamRoleArn);
        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(baseParameters);

        Map<Regions, BaseOutputs> regionConfigOutputsMap = new HashMap<>();
        // for each region, create a config bucket and kms cmk for encrypting environment data
        command.getRegions().forEach(regionName -> {
            Regions region = Regions.fromName(regionName);
            cloudFormationService.createStackAndWait(
                    region,
                    Stack.BASE, parameters, true,
                    command.getTagsDelegate().getTags()
            );
            regionConfigOutputsMap.put(region, configStore.getConfigBucketStackOutputs(region));
        });

        configStore.initializeEnvironment(command.getAdminRoleArn(), cmsIamRoleArn, primaryRegion, regionConfigOutputsMap);
    }

    @Override
    public boolean isRunnable(InitializeEnvironmentCommand command) {
        boolean isRunnable = true;

        if (command.getRegions().size() < 2) {
            log.error("You must supply at least 2 regions so that config and secure data can be encrypted " +
                    "in a highly available manner");
            isRunnable = false;
        }

        if (! command.getRegions().contains(command.getPrimaryRegion())) {
            log.error("The primary region: {} must be in the region collection passed to the command. regions passed: {}",
                    command.getPrimaryRegion(), String.join(", ", command.getRegions()));
            isRunnable = false;
        }

        if (cloudFormationService.isStackPresent(Regions.fromName(command.getPrimaryRegion()),
                Stack.CMS_IAM_ROLE.getFullName(environmentName))) {

            log.error("The IAM Role stack has already been created");
            isRunnable = false;
        }



        return isRunnable;
    }

}
