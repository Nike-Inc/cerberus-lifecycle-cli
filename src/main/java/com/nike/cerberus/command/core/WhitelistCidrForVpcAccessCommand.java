/*
 * Copyright (c) 2016 Nike Inc.
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
import com.nike.cerberus.operation.core.WhitelistCidrForVpcAccessOpertaion;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

/**
 * Command for granting CIDRs ingress to specific ports within the Cerberus VPC.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the base components to support Cerberus.")
public class WhitelistCidrForVpcAccessCommand implements Command {

    public static final String COMMAND_NAME = "whitelist-cidr-for-vpc-access";

    @Parameter(names = "-cidr", description = "One or more CIDRs to be granted ingress on the Cerberus VPC.")
    private List<String> cidrs = new ArrayList<>();

    @Parameter(names = "-port", description = "The ports to grant ingress on within the Cerberus VPC.")
    private List<Integer> ports = new ArrayList<>();

    public List<String> getCidrs() {
        return cidrs;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return WhitelistCidrForVpcAccessOpertaion.class;
    }
}
