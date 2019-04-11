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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.InitializeEnvironmentOperation;

import java.util.List;

import static com.nike.cerberus.command.core.InitializeEnvironmentCommand.COMMAND_NAME;

/**
 * Command for creating the base components for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the IAM roles, KMS keys, and S3 buckets and base config files for needed to " +
                "bootstrap a Cerberus environment and store env infrastructure state")
public class InitializeEnvironmentCommand implements Command {

    public static final String COMMAND_NAME = "init-env";
    public static final String ADMIN_ROLE_ARN_LONG_ARG = "--admin-role-arn";
    public static final String REGION_LONG_ARG = "--regions";
    public static final String PRIMARY_REGION = "--primary-region";

    @Parameter(
            names = ADMIN_ROLE_ARN_LONG_ARG,
            description = "An IAM role ARN that will be given elevated privileges for the KMS CMKs created.",
            required = true
    )
    private String adminRoleArn;

    @Parameter(
            names = REGION_LONG_ARG,
            description = "The regions to use with the Cerberus environment, you must declare at least 2 regions, " +
                    "The CLI and the Management service use the AWS Encryption SDK that supports encrypting payloads " +
                    "with data keys from multiple regions so that the data can be decrypted in other regions in case of " +
                    "region outages. We require at least 2 regions to ensure high availability of config and secure data",
            variableArity = true,
            required = true
    )
    private List<String> regions;

    @Parameter(
            names = PRIMARY_REGION,
            description = "The primary region is the region that will serve traffic by default, this region will " +
                    "have the necessary infrastructure stood up to serve traffic and store data. " +
                    "The primary region must be one of the --region args passed",
            required = true
    )
    private String primaryRegion;

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    public List<String> getRegions() {
        return regions;
    }

    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public CloudFormationParametersDelegate getTagsDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return InitializeEnvironmentOperation.class;
    }

}
