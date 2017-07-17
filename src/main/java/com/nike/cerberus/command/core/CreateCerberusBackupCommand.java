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
import com.nike.cerberus.operation.core.CreateCerberusBackupOperation;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.CreateCerberusBackupCommand.COMMAND_NAME;


/**
 * Command for creating Safe Deposit Box Metadata and Vault secret data backups in S3 encrypted with KMS
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Allows Cerberus operators to create a complete backup in S3 encrypted with KMS. that can be restored with the restore command"
)
public class CreateCerberusBackupCommand implements Command {

    public static final String COMMAND_NAME = "create-backup";

    public static final String BACKUP_REGIONS_LONG_ARG = "--backup-region";
    public static final String BACKUP_REGIONS_SHORT_ARG = "-br";

    @Parameter(
            names = {
                    BACKUP_REGIONS_LONG_ARG,
                    BACKUP_REGIONS_SHORT_ARG
            },
            description = "One or more regions to store backup data in.",
            required = true
    )
    private List<String> backupRegions = new ArrayList<>();

    public List<String> getBackupRegions() {
        return backupRegions;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCerberusBackupOperation.class;
    }
}
