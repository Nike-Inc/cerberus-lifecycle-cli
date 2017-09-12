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
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.SetBackupAdminPrincipalsOperation;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.SetBackupAdminPrincipalsCommand.COMMAND_NAME;

/**
 * Command to update which principals besides for the root account will have permissions to use the backup cmk,
 * AKA create and restore backups.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Update the IAM Principals that are allowed to create and restore backups. " +
                "This command automatically adds by default the root user and configured admin user arn, " +
                "but you can use this command to add iam principals such as CI systems and additional user principals " +
                "that will have access to encrypt and decrypt backup data"
)
public class SetBackupAdminPrincipalsCommand implements Command {

    public static final String COMMAND_NAME = "set-backup-principals";

    public static final String PRINCIPAL_LONG_ARG = "--principal";
    public static final String PRINCIPAL_SHORT_ARG = "-p";

    @Parameter(
            names = {
                    PRINCIPAL_LONG_ARG,
                    PRINCIPAL_SHORT_ARG
            },
            description = "One or more additional principals to grant access to."
    )
    private List<String> additionalPrincipals = new ArrayList<>();

    public List<String> getAdditionalPrincipals() {
        return additionalPrincipals;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return SetBackupAdminPrincipalsOperation.class;
    }
}
