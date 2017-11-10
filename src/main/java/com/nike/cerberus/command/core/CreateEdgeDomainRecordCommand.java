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
import com.nike.cerberus.operation.core.CreateEdgeDomainRecordOperation;

import static com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand.COMMAND_NAME;

/**
 * Command to create the edge domain Route53 record for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the Route53 record for use by Cerberus")
public class CreateEdgeDomainRecordCommand implements Command {

    public static final String COMMAND_NAME = "create-edge-domain-record";

    public static final String BASE_DOMAIN_NAME_LONG_ARG = "--base-domain-name";

    public static final String HOSTED_ZONE_ID_LONG_ARG = "--hosted-zone-id";

    public static final String EDGE_DOMAIN_NAME_OVERRIDE = "edge-domain-name-override";

    @Parameter(names = BASE_DOMAIN_NAME_LONG_ARG,
            description = "The base domain name for Cerberus (e.g. url: https://env.cerberus.example.com => base hostname: cerberus.example.com)",
            required = true)
    private String baseDomainName;

    @Parameter(names = EDGE_DOMAIN_NAME_OVERRIDE,
            description = "The full edge domain name for Cerberus. Default: env.example.domain.com")
    private String edgeDomainNameOverride;

    @Parameter(names = HOSTED_ZONE_ID_LONG_ARG,
            description = "The Route53 Hosted Zone in which to create the new Cerberus record",
            required = true)
    private String hostedZoneId;

    public String getBaseDomainName() {
        return baseDomainName;
    }

    public String getEdgeDomainNameOverride() {
        return edgeDomainNameOverride;
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
        return CreateEdgeDomainRecordOperation.class;
    }

}
