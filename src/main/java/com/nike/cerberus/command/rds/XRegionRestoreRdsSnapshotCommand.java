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

package com.nike.cerberus.command.rds;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.rds.XRegionRestoreRdsSnapshotOperation;

import static com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Restores RDS cluster in the target region for this environment from " +
                "a fresh RDS cluster snapshot created in the source region"
)
public class XRegionRestoreRdsSnapshotCommand implements Command {

    public static final String COMMAND_NAME = "x-region-restore-rds-snapshot";

    public static final String TARGET_REGION_LONG_ARG = "--target-region";

    public static final String SOURCE_REGION_LONG_ARG = "--source-region";

    @Parameter(
            names = TARGET_REGION_LONG_ARG,
            description = "The AWS Region to restore RDS cluster snapshot in",
            required = true
    )
    private String targetRegion;

    public String getTargetRegion() {
        return targetRegion;
    }

    @Parameter(
            names = SOURCE_REGION_LONG_ARG,
            description = "The AWS Region to create RDS cluster snapshot in",
            required = true
    )
    private String sourceRegion;

    public String getSourceRegion() {
        return sourceRegion;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return XRegionRestoreRdsSnapshotOperation.class;
    }
}
