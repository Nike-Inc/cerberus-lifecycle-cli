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
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateBaseOperation;

import static com.nike.cerberus.command.core.CreateBaseCommand.COMMAND_NAME;

/**
 * Command for creating the base components for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the IAM roles, KMS keys, and S3 buckets for Cerberus")
public class CreateBaseCommand implements Command {

    public static final String COMMAND_NAME = "create-base";

    public static final String ADMIN_ROLE_ARN_LONG_ARG = "--admin-role-arn";

    @Parameter(names = ADMIN_ROLE_ARN_LONG_ARG,
            description = "A IAM role ARN that will be given elevated privileges for the KMS CMK created.",
            required = true)
    private String adminRoleArn;

    @ParametersDelegate
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    public TagParametersDelegate getTagsDelegate() {
        return tagParameters;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateBaseOperation.class;
    }

}
