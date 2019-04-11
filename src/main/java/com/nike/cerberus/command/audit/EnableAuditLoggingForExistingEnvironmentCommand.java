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

package com.nike.cerberus.command.audit;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.EnableAuditLoggingForExistingEnvironmentOperation;

import static com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand.COMMAND_DESCRIPTION;
import static com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = COMMAND_DESCRIPTION
)
public class EnableAuditLoggingForExistingEnvironmentCommand implements Command {

    public static final String COMMAND_NAME = "enable-audit-logging-for-existing-environment";
    public static final String COMMAND_DESCRIPTION =
            "A Composite command that will will execute the following commands in order: "
                    + "update-stack --stack-name iam-roles --overwrite-template, "
                    + "create-audit-logging-stack, "
                    + "create-audit-log-athena-db-and-table, "
                    + "enable-audit-logging, "
                    + "update-cms-config, "
                    + "reboot-cms. "
                    + "This will do everything required to enable audit logging for an existing environment.";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return EnableAuditLoggingForExistingEnvironmentOperation.class;
    }
}
