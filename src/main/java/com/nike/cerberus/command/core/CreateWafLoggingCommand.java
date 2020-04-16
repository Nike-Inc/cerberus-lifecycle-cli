/*
 * Copyright (c) 2020 Nike, Inc.
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
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateWafLoggingOperation;

import static com.nike.cerberus.command.core.CreateWafCommand.COMMAND_NAME;

/**
 * Command to create the WAF logging for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the Web Application Firewall (WAF) logging.")
public class CreateWafLoggingCommand implements Command {

    public static final String COMMAND_NAME = "create-waf-logging";

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    @Parameter(names = {"--skip-stack-creation", "-s"}, description = "Skips WAF logging stack creation.")
    private boolean skipStackCreation;

    public boolean isSkipStackCreation() {
        return skipStackCreation;
    }

    @Parameter(names = {"--s3-prefix"}, description = "The prefix of WAF logs in S3 bucket.")
    private String s3Prefix = "firehose/";

    public String getS3Prefix() {
        return s3Prefix;
    }

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateWafLoggingOperation.class;
    }

}
