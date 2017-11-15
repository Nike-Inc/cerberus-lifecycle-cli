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

package com.nike.cerberus.command.composite;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.composite.CreateCerberusEnvironmentOperation;

import static com.nike.cerberus.command.composite.CreateCerberusEnvironmentCommand.COMMAND_NAME;

@Parameters(
        commandNames = {
                COMMAND_NAME
        },
        commandDescription = "Creates a complete Cerberus environment from scratch using an environment yaml"
)
public class CreateCerberusEnvironmentCommand implements Command {

    public static final String COMMAND_NAME = "create-cerberus-environment";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCerberusEnvironmentOperation.class;
    }
}
