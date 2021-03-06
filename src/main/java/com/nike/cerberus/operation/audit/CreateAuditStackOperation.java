/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.operation.audit;

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.audit.CreateAuditLoggingStackCommand;
import com.nike.cerberus.domain.cloudformation.AuditParameters;
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


public class CreateAuditStackOperation implements Operation<CreateAuditLoggingStackCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final String environmentName;
    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateAuditStackOperation(CloudFormationService cloudFormationService,
                                     ConfigStore configStore,
                                     @Named(ENV_NAME) String environmentName,
                                     CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.environmentName = environmentName;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(CreateAuditLoggingStackCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());
        AuditParameters auditParameters = new AuditParameters()
                .setAccountAdminArn(command.getAdminRoleArn())
                .setCmsIamRoleArn(configStore.getCmsIamRoleOutputs().getCmsIamRoleArn())
                .setEnvironmentName(environmentName);
        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(auditParameters);
        cloudFormationService.createStackAndWait(region, Stack.AUDIT, parameters, true, command.getCloudFormationParametersDelegate().getTags());
    }


    @Override
    public boolean isRunnable(CreateAuditLoggingStackCommand command) {
        boolean isRunnable = true;
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());

        if (cloudFormationService.isStackPresent(region, Stack.AUDIT.getFullName(environmentName))) {
            log.error("The audit stack already exists use the update-stack command");
            isRunnable = false;
        }

        return isRunnable;
    }
}
