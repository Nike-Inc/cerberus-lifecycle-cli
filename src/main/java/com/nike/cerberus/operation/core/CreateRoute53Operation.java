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

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.Route53Parameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Route53Service;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the origin and load balancer Route53 records for Cerberus
 */
public class CreateRoute53Operation implements Operation<CreateRoute53Command> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Route53Service route53Service;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateRoute53Operation(final EnvironmentMetadata environmentMetadata,
                                  final CloudFormationService cloudFormationService,
                                  final Route53Service route53Service,
                                  @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.route53Service = route53Service;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateRoute53Command command) {
        final String environmentName = environmentMetadata.getName();

        final Route53Parameters route53Parameters = new Route53Parameters()
                .setHostedZoneId(command.getHostedZoneId())
                .setLoadBalancerDomainName(getLoadBalancerDomainName(command.getBaseDomainName(), command.getLoadBalancerDomainNameOverride()))
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName))
                .setOriginDomainName(getOriginDomainName(command.getBaseDomainName(), command.getOriginDomainNameOverride()));

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(route53Parameters, typeReference);

        final String stackId = cloudFormationService.createStack(Stack.ROUTE53, parameters, true,
                command.getTagsDelegate().getTags());

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (StackStatus.CREATE_COMPLETE != endStatus) {
            throw new UnexpectedCloudFormationStatusException(String.format("Unexpected end status: %s", endStatus.name()));
        }
    }

    @Override
    public boolean isRunnable(final CreateRoute53Command command) {
        final String environmentName = environmentMetadata.getName();
        final String loadBalancerDomainName = getLoadBalancerDomainName(command.getBaseDomainName(), command.getLoadBalancerDomainNameOverride());
        final String originDomainName = getOriginDomainName(command.getBaseDomainName(), command.getOriginDomainNameOverride());

        if (!cloudFormationService.isStackPresent(Stack.LOAD_BALANCER.getFullName(environmentName))) {
            throw new IllegalStateException("The load balancer stack must exist to create the Route53 record!");
        }
        if (cloudFormationService.isStackPresent(Stack.ROUTE53.getFullName(environmentName))) {
            throw new IllegalStateException("Route53 stack already exists.");
        }

        return !(route53Service.recordSetWithNameAlreadyExists(loadBalancerDomainName, command.getHostedZoneId()) ||
                route53Service.recordSetWithNameAlreadyExists(originDomainName, command.getHostedZoneId()));
    }

    private String getLoadBalancerDomainName(final String baseDomainName, final String loadBalancerDomainNameOverride) {
        final String defaultLoadBalancerDomainName = String.format("%s.%s.%s",
                environmentMetadata.getName(),
                environmentMetadata.getRegionName(),
                baseDomainName);

        return StringUtils.isBlank(loadBalancerDomainNameOverride) ?
                defaultLoadBalancerDomainName : loadBalancerDomainNameOverride;
    }

    private String getOriginDomainName(final String baseDomainName, final String originDomainNameOverride) {
        final String defaultOriginDomainName = String.format("origin.%s.%s",
                environmentMetadata.getName(),
                baseDomainName);

        return StringUtils.isBlank(originDomainNameOverride) ?
                defaultOriginDomainName : originDomainNameOverride;
    }
}
