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
import com.nike.cerberus.operation.core.RollingRebootWithHealthCheckOperation;

import static com.nike.cerberus.command.core.RollingRebootWithHealthCheckCommand.COMMAND_NAME;

/**
 * Command to reboot the CMS cluster.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Performs a safe rolling reboot on instances in the given cluster, checking that " +
                "the previous instance is healthy before rebooting the next one."
)
public class RollingRebootWithHealthCheckCommand implements Command {

    public static final String COMMAND_NAME = "rolling-reboot";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to reboot.")
    private Stack stack = Stack.CMS;

    public Stack getStack() {
        return stack;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return RollingRebootWithHealthCheckOperation.class;
    }
}
