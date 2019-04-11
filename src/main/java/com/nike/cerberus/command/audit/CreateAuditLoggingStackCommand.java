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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.CreateAuditStackOperation;

import static com.nike.cerberus.command.audit.CreateAuditLoggingStackCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Creates an S3 bucket and IAM roles configured to allow CMS to write audit log data and " +
                "IAM role that allows AWS Athena/Glue queries"
)
public class CreateAuditLoggingStackCommand implements Command {

    public static final String COMMAND_NAME = "create-audit-logging-stack";

    public static final String ADMIN_ROLE_ARN_LONG_ARG = "--admin-role-arn";

    @Parameter(
            names = ADMIN_ROLE_ARN_LONG_ARG,
            description = "An IAM role ARN that will be given elevated privileges for the KMS CMKs created.",
            required = true
    )
    private String adminRoleArn;

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateAuditStackOperation.class;
    }
}
