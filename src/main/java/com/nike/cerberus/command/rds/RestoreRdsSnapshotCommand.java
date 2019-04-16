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
import com.nike.cerberus.operation.rds.RestoreRdsSnapshotOperation;
import org.apache.commons.lang3.StringUtils;

import static com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand.COMMAND_NAME;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Restore RDS cluster snapshot in a specified region by updating cloudformation stack"
)
public class RestoreRdsSnapshotCommand implements Command {

    public static final String COMMAND_NAME = "restore-rds-snapshot";

    public static final String SNAPSHOT_IDENTIFIER = "--snapshot-identifier";

    public static final String REGION_ENV_KEY = "RESTORE_RDS_SNAPSHOT_REGION";
    public static final String RDS_SNAPSHOT_ENV_KEY = "RESTORE_RDS_SNAPSHOT_IDENTIFIER";

    @Parameter(
            names = {STACK_REGION},
            description = "Region to restore RDS cluster snapshot in. " +
                    "Defaults to environment variable RESTORE_RDS_SNAPSHOT_REGION"
    )
    private String region;

    public String getRegion() {
        if (StringUtils.isBlank(region)) {
            return System.getenv(REGION_ENV_KEY);
        } else {
            return region;
        }
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return RestoreRdsSnapshotOperation.class;
    }

    @Parameter(
            names = SNAPSHOT_IDENTIFIER,
            description = " Restore from the snapshot by updating cloudformation stack with snapshot identifier. " +
                    "Defaults to environment variable RESTORE_RDS_SNAPSHOT_IDENTIFIER"
    )
    private String snapshotIdentifier;

    public String getSnapshotIdentifier() {
        if (StringUtils.isBlank(snapshotIdentifier)) {
            return System.getenv(RDS_SNAPSHOT_ENV_KEY);
        } else {
            return snapshotIdentifier;
        }
    }
}
