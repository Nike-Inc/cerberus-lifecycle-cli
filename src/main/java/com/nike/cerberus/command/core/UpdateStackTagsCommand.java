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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.UpdateStackTagsOperation;
import com.nike.cerberus.util.StackConverter;

import static com.nike.cerberus.command.core.UpdateStackTagsCommand.COMMAND_NAME;


/**
 * Command for updating the specified CloudFormation stack with tags.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the specified CloudFormation stack tags.")
public class UpdateStackTagsCommand implements Command {

    public static final String COMMAND_NAME = "update-stack-tags";
    public static final String STACK_NAME_LONG_ARG = "--stack-name";
    public static final String OVERWRITE_TAGS_LONG_ARG = "--overwrite-tags";

    @Parameter(names = {STACK_NAME_LONG_ARG}, required = true, description = "The stack name to update.", converter = StackConverter.class)
    private Stack stack;

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    @Parameter(names = OVERWRITE_TAGS_LONG_ARG,
            description = "Flag for overwriting existing CloudFormation template")
    private boolean overwriteTags;

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isOverwriteTags() {
        return overwriteTags;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateStackTagsOperation.class;
    }
}
