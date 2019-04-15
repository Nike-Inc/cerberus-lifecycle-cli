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
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.rds.CreateDatabaseOperation;

import static com.nike.cerberus.command.rds.CreateDatabaseCommand.COMMAND_NAME;

/**
 * Command to create the database for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the database to be used by the Cerberus Management Service (CMS)")
public class CreateDatabaseCommand implements Command {

    public static final String COMMAND_NAME = "create-database";

    public static final String INSTANCE_CLASS_LONG_ARG = "--instance-class";

    public static final String RESTORE_FROM_SNAPSHOT = "--restore-from-snapshot-using-identifier";

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Parameter(names = INSTANCE_CLASS_LONG_ARG,
            description = "The Instance Class to use, defaults to db.r3.large"
    )
    private String instanceClass;

    public String getInstanceClass() {
        return instanceClass;
    }

    @Parameter(
            names = RESTORE_FROM_SNAPSHOT,
            description = "option for setting a snapshot identifier on the RDS stack to restore from the snapshot " +
                    "while standing up the new RDS cluster via cloudformation"
    )
    private String snapshotIdentifier;

    public String getSnapshotIdentifier() {
        return snapshotIdentifier;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateDatabaseOperation.class;
    }

}
