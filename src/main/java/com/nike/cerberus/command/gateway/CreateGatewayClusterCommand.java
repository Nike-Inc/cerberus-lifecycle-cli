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

package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.CreateGatewayClusterOperation;

import static com.nike.cerberus.command.gateway.CreateGatewayClusterCommand.COMMAND_NAME;

/**
 * Command to create the Gateway cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the CMS cluster.")
public class CreateGatewayClusterCommand implements Command {

    public static final String COMMAND_NAME = "create-gateway-cluster";

    @Parameter(names = "--hosted-zone-id",
            description = "The Route 53 hosted zone ID that will be used to create the CNAME record for Cerberus.",
            required = true)
    private String hostedZoneId;

    @Parameter(names = "--hostname",
            description = "The hostname that will be used to expose this Cerberus environment.",
            required = true)
    private String hostname;

    @ParametersDelegate
    private StackDelegate stackDelegate = new StackDelegate();

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public String getHostname() {
        return hostname;
    }

    public StackDelegate getStackDelegate() {
        return stackDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateGatewayClusterOperation.class;
    }
}
