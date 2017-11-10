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

package com.nike.cerberus.operation.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.Route53Parameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateRoute53Operation implements Operation<CreateRoute53Command> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateRoute53Operation(final EnvironmentMetadata environmentMetadata,
                                       final CloudFormationService cloudFormationService,
                                       @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateRoute53Command command) {
        final String environmentName = environmentMetadata.getName();

        final Route53Parameters route53Parameters = new Route53Parameters()
                .setHostname(command.getCerberusHostname())
                .setHostedZoneId(command.getHostedZoneId())
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName));

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(route53Parameters, typeReference);

        cloudFormationService.createStack(Stack.ROUTE53, parameters, true, command.getTagsDelegate().getTags());
    }

    @Override
    public boolean isRunnable(final CreateRoute53Command command) {
        String environmentName = environmentMetadata.getName();
        try {
            cloudFormationService.getStackId(Stack.LOAD_BALANCER.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("The load balancer stack must exist to create the Route53 record!", iae);
        }

        return ! cloudFormationService.isStackPresent(Stack.ROUTE53.getFullName(environmentName));
    }
}
