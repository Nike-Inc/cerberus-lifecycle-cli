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

import com.amazonaws.regions.Regions;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.WhitelistCidrForVpcAccessOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION_DESCRIPTION;

/**
 * Command for granting CIDRs ingress to specific ports within the Cerberus VPC.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the base components to support Cerberus.")
public class WhitelistCidrForVpcAccessCommand implements Command {

    public static final String COMMAND_NAME = "whitelist-cidr-for-vpc-access";
    public static final String CIDR_LONG_ARG = "-cidr";
    public static final String PORT_LONG_ARG = "-port";

    @Parameter(names = CIDR_LONG_ARG, description = "One or more CIDRs to be granted ingress on the Cerberus VPC.")
    private List<String> cidrs = new ArrayList<>();

    @Parameter(names = PORT_LONG_ARG, description = "The ports to grant ingress on within the Cerberus VPC.")
    private List<Integer> ports = new ArrayList<>();

    @Parameter(names = STACK_REGION, description = STACK_REGION_DESCRIPTION)
    private String stackRegion;

    public List<String> getCidrs() {
        return cidrs;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public Optional<Regions> getStackRegion() {
        return stackRegion == null ? Optional.empty() : Optional.of(Regions.fromName(stackRegion));
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return WhitelistCidrForVpcAccessOperation.class;
    }
}
