/*
 * Copyright (c) 2021 Nike, Inc.
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
import com.nike.cerberus.command.core.CreateInstanceProfileCommand;
import com.nike.cerberus.domain.cloudformation.InstanceProfileParameters;
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

public class CreateInstanceProfileOperation implements Operation<CreateInstanceProfileCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final String environmentName;

    @Inject
    public CreateInstanceProfileOperation(CloudFormationService cloudFormationService,
                                          ConfigStore configStore,
                                          CloudFormationObjectMapper cloudFormationObjectMapper,
                                          @Named(ENV_NAME) String environmentName) {
        this.environmentName = environmentName;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
    }

    @Override
    public void run(CreateInstanceProfileCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());

        String cmsIamRoleName = command.getInstanceProfileIamRole()
                .orElse(configStore.getCmsIamRoleOutputs(configStore.getPrimaryRegion()).getCmsIamRoleName());

        InstanceProfileParameters instanceProfileParameters = new InstanceProfileParameters()
                .setCmsIamRoleName(cmsIamRoleName);

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(instanceProfileParameters);

        cloudFormationService.createStackAndWait(
                region,
                Stack.INSTANCE_PROFILE,
                parameters, true,
                command.getCloudFormationParametersDelegate().getTags());
    }

    @Override
    public boolean isRunnable(CreateInstanceProfileCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());

        boolean isRunnable = true;

        if (!cloudFormationService.isStackPresent(region,
                Stack.IAM_ROLES.getFullName(environmentName)) && !command.getInstanceProfileIamRole().isPresent()) {
            logger.error("The iam role stack must exist to create the instance profile");
            isRunnable = false;
        }

        if (cloudFormationService.isStackPresent(region,
                Stack.INSTANCE_PROFILE.getFullName(environmentName))) {
            logger.error("The instance profile stack already exists");
            isRunnable = false;
        }
        return isRunnable;
    }
}
