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

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;

import static com.nike.cerberus.command.core.UpdateBackupCmkAdminPrincipalsCommand.COMMAND_NAME;

/**
 * Command to update which principals besides for the root account will have permissions to use the backup cmk,
 * AKA create and restore backups.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Update the IAM Principals that are allowed to create and restore backups."
)
public class UpdateBackupCmkAdminPrincipalsCommand implements Command {

    public static final String COMMAND_NAME = "";

    @Override
    public String getCommandName() {
        return null;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return null;
    }
}
