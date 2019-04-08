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
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateRoute53Operation;

import static com.nike.cerberus.command.core.CreateRoute53Command.COMMAND_NAME;

/**
 * Creates the origin and load balancer Route53 records for Cerberus
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the Route53 record for use by Cerberus")
public class CreateRoute53Command implements Command {

    public static final String COMMAND_NAME = "create-route53-stack";

    public static final String BASE_DOMAIN_NAME_LONG_ARG = "--base-domain-name";

    public static final String HOSTED_ZONE_ID_LONG_ARG = "--hosted-zone-id";

    public static final String ORIGIN_DOMAIN_NAME_OVERRIDE = "--origin-domain-name-override";

    public static final String LOAD_BALANCER_DOMAIN_NAME_OVERRIDE = "--load-balancer-domain-name-override";

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Parameter(names = BASE_DOMAIN_NAME_LONG_ARG,
            description = "The base hostname for Cerberus (e.g. url: https://env.cerberus.example.com => base hostname: cerberus.example.com)",
            required = true)
    private String baseDomainName;

    @Parameter(names = HOSTED_ZONE_ID_LONG_ARG,
            description = "The Route53 Hosted Zone in which to create the new Cerberus record",
            required = true)
    private String hostedZoneId;

    @Parameter(names = LOAD_BALANCER_DOMAIN_NAME_OVERRIDE,
            description = "Override the load balancer domain name for Cerberus. Default: env.region.example.domain.com")
    private String loadBalancerDomainNameOverride;

    @Parameter(names = ORIGIN_DOMAIN_NAME_OVERRIDE,
            description = "Override the origin domain name for Cerberus. Default: origin.env.example.domain.com")
    private String originDomainNameOverride;

    public String getBaseDomainName() {
        return baseDomainName;
    }

    public String getOriginDomainNameOverride() {
        return originDomainNameOverride;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public String getLoadBalancerDomainNameOverride() {
        return loadBalancerDomainNameOverride;
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
