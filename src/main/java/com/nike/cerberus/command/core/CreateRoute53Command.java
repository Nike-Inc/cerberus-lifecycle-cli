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
import com.nike.cerberus.operation.core.CreateRoute53Operation;

import static com.nike.cerberus.command.core.CreateRoute53Command.COMMAND_NAME;

/**
 * Command to create the Route53 record for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the Route53 record for use by Cerberus")
public class CreateRoute53Command implements Command {

    public static final String COMMAND_NAME = "create-route53-record";

    public static final String HOSTNAME_LONG_ARG = "--hostname";

    public static final String HOSTED_ZONE_ID = "--hosted-zone-id";

    @Parameter(names = HOSTNAME_LONG_ARG,
            description = "The hostname of the Route53 record to be created for Cerberus (e.g. <env>.cerberus.example.com)")
    private String cerberusHostname;

    @Parameter(names = HOSTED_ZONE_ID,
            description = "The Route53 Hosted Zone in which to create the new Cerberus record")
    private String hostedZoneId;

    public String getCerberusHostname() {
        return cerberusHostname;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateRoute53Operation.class;
    }

}
