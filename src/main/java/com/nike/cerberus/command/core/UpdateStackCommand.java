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

package com.nike.cerberus.command.core;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.UpdateStackOperation;
import com.nike.cerberus.util.StackConverter;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.core.UpdateStackCommand.COMMAND_NAME;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_ARG;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_DESCRIPTION;

/**
 * Command for updating the specified CloudFormation stack with the new parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the specified CloudFormation stack.")
public class UpdateStackCommand implements Command {

    public static final String COMMAND_NAME = "update-stack";
    public static final String STACK_NAME_LONG_ARG = "--stack-name";
    public static final String CF_TEMPLATE_FILE_NAME = "--cf-template-file-name";
    public static final String OVERWRITE_TEMPLATE_LONG_ARG = "--overwrite-template";
    public static final String PARAMETER_SHORT_ARG = "-P";

    @Parameter(names = {STACK_NAME_LONG_ARG}, required = true, description = "The stack name to update.", converter = StackConverter.class)
    private Stack stack;

    @Parameter(names = {CF_TEMPLATE_FILE_NAME}, description = "Alternate template file name, e.g. custom.yaml")
    private String cfTemplateFileName;

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Parameter(names = OVERWRITE_TEMPLATE_LONG_ARG,
            description = "Flag for overwriting existing CloudFormation template")
    private boolean overwriteTemplate;

    @DynamicParameter(names = PARAMETER_SHORT_ARG, description = "Dynamic parameters for overriding the values for specific parameters in the CloudFormation.")
    private Map<String, String> dynamicParameters = new HashMap<>();

    public Stack getStack() {
        return stack;
    }

    public String getCfTemplateFileName() {return cfTemplateFileName;}

    public boolean isOverwriteTemplate() {
        return overwriteTemplate;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Parameter(names = SKIP_AMI_TAG_CHECK_ARG,
            description = SKIP_AMI_TAG_CHECK_DESCRIPTION)
    private boolean skipAmiTagCheck;

    public boolean isSkipAmiTagCheck() {
        return skipAmiTagCheck;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateStackOperation.class;
    }
}
