/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.command.rds;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.rds.CleanUpRdsSnapshotsOperation;

import static com.nike.cerberus.command.rds.CleanUpRdsSnapshotsCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Copies RDS cluster snapshots in the primary for this environment to the secondary regions"
)
public class CleanUpRdsSnapshotsCommand implements Command {

    public static final String COMMAND_NAME = "cleanup-rds-snapshots";

    public static final String DAYS_LONG_ARG = "--days";

    public static final String DRY_LONG_ARG = "--dry";

    @Parameter(
            names = DAYS_LONG_ARG,
            description = "How old cross region snapshot copies can be before they are deleted"
    )
    private int days = 14;

    public int getDays() {
        return days;
    }

    @Parameter(
            names = DRY_LONG_ARG,
            description = "Add this flag to list the snapshots that would be deleted"
    )
    boolean dryRun = false;

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CleanUpRdsSnapshotsOperation.class;
    }
}
