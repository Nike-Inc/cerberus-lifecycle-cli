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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.domain.cloudformation.StackInfo;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AutoScalingService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CONFIG_OBJECT_MAPPER;
import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Prints the parameters and outputs for the specified component.
 */
public class PrintStackInfoOperation implements Operation<PrintStackInfoCommand> {

    private static final String ASG_LOGICAL_ID_OUTPUT_NAME = "autoscalingGroupLogicalId";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final AutoScalingService autoScalingService;

    private final ObjectMapper objectMapper;

    private final ConfigStore configStore;

    @Inject
    public PrintStackInfoOperation(@Named(ENV_NAME) String environmentName,
                                   CloudFormationService cloudFormationService,
                                   AutoScalingService autoScalingService,
                                   @Named(CONFIG_OBJECT_MAPPER) ObjectMapper objectMapper,
                                   ConfigStore configStore) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.autoScalingService = autoScalingService;
        this.objectMapper = objectMapper;
        this.configStore = configStore;
    }

    @Override
    public void run(final PrintStackInfoCommand command) {
        String stackId = command.getStack().getFullName(environmentName);

        if (StringUtils.isBlank(stackId) ||
                !cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), stackId)) {

            logger.error("The specified environment doesn't contain a stack for " + command.getStack().getName());
            return;
        }

        Map<String, String> stackParameters = cloudFormationService.getStackParameters(configStore.getPrimaryRegion(), stackId);
        Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(configStore.getPrimaryRegion(), stackId);
        StackInfo stackInfo = new StackInfo();
        stackInfo.setStackId(stackId).setStackParameters(stackParameters).setStackOutputs(stackOutputs);
        String autoscalingGroupLogicalId = stackOutputs.get(ASG_LOGICAL_ID_OUTPUT_NAME);

        if (StringUtils.isNotBlank(autoscalingGroupLogicalId)) {
            List<String> publicDnsForAutoScalingGroup =
                    autoScalingService.getPublicDnsForAutoScalingGroup(autoscalingGroupLogicalId);
            stackInfo.setPublicDnsForInstances(publicDnsForAutoScalingGroup);
        }

        try {
            logger.info(objectMapper.writeValueAsString(stackInfo));
        } catch (JsonProcessingException e) {
            logger.error("Unable to convert the stack data to JSON.", e);
        }
    }

    @Override
    public boolean isRunnable(final PrintStackInfoCommand command) {
        return true;
    }
}
