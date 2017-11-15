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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.DeleteStackOperation;
import com.nike.cerberus.util.StackConverter;

import static com.nike.cerberus.command.core.DeleteStackCommand.COMMAND_NAME;

@Parameters(
        commandNames = {
                COMMAND_NAME
        },
        commandDescription = "Uses CloudFormation to delete a Stack"
)
public class DeleteStackCommand implements Command {

    public static final String COMMAND_NAME = "delete-stack";

    public static final String STACK_NAME_LONG_ARG = "--stack-name";

    @Parameter(
            names = {STACK_NAME_LONG_ARG},
            required = true,
            description = "Stack name to delete",
            converter = StackConverter.class)
    private Stack stack;

    public Stack getStack() {
        return stack;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return DeleteStackOperation.class;
    }
}
