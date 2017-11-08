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
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateSecurityGroupsOperation;

import static com.nike.cerberus.command.core.CreateSecurityGroupsCommand.COMMAND_NAME;

/**
 * Command to create the security groups for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the IAM roles, KMS keys, and S3 buckets for Cerberus")
public class CreateSecurityGroupsCommand implements Command {

    public static final String COMMAND_NAME = "create-security-groups";

    public static final String LOAD_BALANCER_CIDR_BLOCK_LONG_ARG = "--load-balancer-cidr";

    @Parameter(names = LOAD_BALANCER_CIDR_BLOCK_LONG_ARG,
            description = "The CIDR from which to allow traffic to the load balancer")
    private String loadBalancerCidr;

    public String getLoadBalancerCidr() {
        return loadBalancerCidr;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateSecurityGroupsOperation.class;
    }

}
