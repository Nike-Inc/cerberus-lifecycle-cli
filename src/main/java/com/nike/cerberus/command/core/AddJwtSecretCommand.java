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
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.AddJwtSecretOperation;

import static com.nike.cerberus.command.core.AddJwtSecretCommand.COMMAND_NAME;

/**
 * Command for add/rotate JWT secret for CMS
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Add/rotate JWT secret for CMS")
public class AddJwtSecretCommand implements Command {

    public static final String COMMAND_NAME = "add-jwt-secret";
    public static final String ACTIVATION_DELAY_LONG_ARG = "--activation-delay";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Parameter(names = ACTIVATION_DELAY_LONG_ARG, description = "delay in second before the secret can be used to sign JWT")
    private long activationDelay = 5 * 60;

    public long getActivationDelay() {
        return activationDelay;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return AddJwtSecretOperation.class;
    }
}
