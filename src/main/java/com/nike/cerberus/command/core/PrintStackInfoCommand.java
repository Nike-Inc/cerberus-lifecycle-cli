/*
 * Copyright (c) 2016 Nike, Inc.
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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.StackInfo;
import com.nike.cerberus.domain.environment.StackName;
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

import static com.nike.cerberus.command.core.PrintStackInfoCommand.COMMAND_NAME;
import static com.nike.cerberus.module.CerberusModule.CONFIG_OBJECT_MAPPER;

/**
 * Command for printing information about the specified CloudFormation stack.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Prints information about the CloudFormation stack.")
public class PrintStackInfoCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "print-stack-info";

    private static final String ASG_LOGICAL_ID_OUTPUT_NAME = "autoscalingGroupLogicalId";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to print information about.")
    private StackName stackName;

    private final ConfigStore configStore;

    private final CloudFormationService cloudFormationService;

    private final AutoScalingService autoScalingService;

    private final ObjectMapper objectMapper;

    @Inject
    public PrintStackInfoCommand(final ConfigStore configStore,
                                 final CloudFormationService cloudFormationService,
                                 final AutoScalingService autoScalingService,
                                 @Named(CONFIG_OBJECT_MAPPER) final ObjectMapper objectMapper) {
        this.configStore = configStore;
        this.cloudFormationService = cloudFormationService;
        this.autoScalingService = autoScalingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute() {
        final String stackId = configStore.getStackId(stackName);

        if (StringUtils.isBlank(stackId) || !cloudFormationService.isStackPresent(stackId)) {
            logger.error("The specified environment doesn't contain a stack for " + stackName.getName());
            return;
        }

        final Map<String, String> stackParameters = cloudFormationService.getStackParameters(stackId);
        final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);
        final StackInfo stackInfo = new StackInfo();
        stackInfo.setStackId(stackId).setStackParameters(stackParameters).setStackOutputs(stackOutputs);
        final String autoscalingGroupLogicalId = stackOutputs.get(ASG_LOGICAL_ID_OUTPUT_NAME);

        if (StringUtils.isNotBlank(autoscalingGroupLogicalId)) {
            final List<String> publicDnsForAutoScalingGroup =
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
    public boolean isRunnable() {
        return true;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
}
