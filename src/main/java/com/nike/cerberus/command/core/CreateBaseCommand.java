/*
 * Copyright (c) 2016 Nike, Inc.
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
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateBaseOperation;

import static com.nike.cerberus.command.core.CreateBaseCommand.COMMAND_NAME;

/**
 * Command for creating the base components for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the base components to support Cerberus.")
public class CreateBaseCommand implements Command {

    public static final String COMMAND_NAME = "create-base";
    public static final String ADMIN_ROLE_ARN_LONG_ARG = "--admin-role-arn";
    public static final String VPC_HOSTED_ZONE_NAME_LONG_ARG = "--vpc-hosted-zone-name";
    public static final String OWNER_EMAIL_LONG_ARG = "--owner-email";
    public static final String COST_CENTER_LONG_ARG = "--costcenter";

    @Parameter(names = ADMIN_ROLE_ARN_LONG_ARG,
            description = "A IAM role ARN that will be given elevated privileges for the KMS CMK created.",
            required = true)
    private String adminRoleArn;

    @Parameter(names = VPC_HOSTED_ZONE_NAME_LONG_ARG,
            description = "The Route 53 hosted zone name that will be created for CNAME records used by internal ELBs.",
            required = true)
    private String vpcHostedZoneName;

    @Parameter(names = OWNER_EMAIL_LONG_ARG,
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.",
            required = true)
    private String ownerEmail;

    @Parameter(names = COST_CENTER_LONG_ARG,
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.",
            required = true)
    private String costcenter;

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    public String getVpcHostedZoneName() {
        return vpcHostedZoneName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCostcenter() {
        return costcenter;
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
