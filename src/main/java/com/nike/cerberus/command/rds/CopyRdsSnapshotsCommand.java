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

package com.nike.cerberus.command.rds;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.rds.CopyRdsSnapshotsOperation;

import static com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Copies rds cluster snapshots in the primary for this environment to the secondary regions"
)
public class CopyRdsSnapshotsCommand implements Command {

    public static final String COMMAND_NAME = "copy-rds-snapshots";

    public static final String DAYS_LONG_ARG = "--days";

    @Parameter(
            names = DAYS_LONG_ARG,
            description = "How old a rds cluster snapshot can be and still be considered for copying, defaults to 1 day"
    )
    private long days = 1;

    public long getDays() {
        return days;
    }


    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CopyRdsSnapshotsOperation.class;
    }
}
