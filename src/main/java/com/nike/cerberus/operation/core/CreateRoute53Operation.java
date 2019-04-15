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

package com.nike.cerberus.operation.core;

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.domain.cloudformation.Route53Parameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Route53Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the origin and load balancer Route53 records for Cerberus
 */
public class CreateRoute53Operation implements Operation<CreateRoute53Command> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final Route53Service route53Service;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final ConfigStore configStore;

    @Inject
    public CreateRoute53Operation(@Named(ENV_NAME) String environmentName,
                                  CloudFormationService cloudFormationService,
                                  Route53Service route53Service,
                                  CloudFormationObjectMapper cloudFormationObjectMapper,
                                  ConfigStore configStore) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.route53Service = route53Service;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.configStore = configStore;
    }

    @Override
    public void run(CreateRoute53Command command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
            .orElse(configStore.getPrimaryRegion());

        Route53Parameters route53Parameters = new Route53Parameters()
                .setHostedZoneId(command.getHostedZoneId())
                .setLoadBalancerDomainName(getLoadBalancerDomainName(command.getBaseDomainName(), command.getLoadBalancerDomainNameOverride(), region))
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName))
                .setOriginDomainName(getOriginDomainName(command.getBaseDomainName(), command.getOriginDomainNameOverride(), region));

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(route53Parameters);

        cloudFormationService.createStackAndWait(
            region,
                Stack.ROUTE53,
                parameters,
                true,
                command.getCloudFormationParametersDelegate().getTags());
    }

    @Override
    public boolean isRunnable(CreateRoute53Command command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
            .orElse(configStore.getPrimaryRegion());
        String loadBalancerDomainName = getLoadBalancerDomainName(command.getBaseDomainName(), command.getLoadBalancerDomainNameOverride(), region);
        String originDomainName = getOriginDomainName(command.getBaseDomainName(), command.getOriginDomainNameOverride(), region);

        boolean isRunnable = true;
        if (!cloudFormationService.isStackPresent(region,
                Stack.LOAD_BALANCER.getFullName(environmentName))) {
            logger.error("The load balancer stack must exist to create the Route53 record!");
            isRunnable = false;
        }
        if (cloudFormationService.isStackPresent(region,
                Stack.ROUTE53.getFullName(environmentName))) {
            logger.error("Route53 stack already exists.");
            isRunnable = false;
        }
        if (route53Service.getRecordSetByName(loadBalancerDomainName, command.getHostedZoneId(), region).isPresent()) {
            logger.error("The load balancer name is already registered");
            isRunnable = false;
        }
        if (route53Service.getRecordSetByName(originDomainName, command.getHostedZoneId(), region).isPresent()) {
            logger.error("The origin name is already registered");
            isRunnable = false;
        }

        return isRunnable;
    }

    private String getLoadBalancerDomainName(String baseDomainName, String loadBalancerDomainNameOverride, Regions region) {
        String defaultLoadBalancerDomainName = String.format("%s.%s.%s",
                environmentName,
                region.getName(),
                baseDomainName);

        return StringUtils.isBlank(loadBalancerDomainNameOverride) ?
                defaultLoadBalancerDomainName : loadBalancerDomainNameOverride;
    }

    private String getOriginDomainName(String baseDomainName, String originDomainNameOverride, Regions region) {
        String defaultOriginDomainName = String.format("origin.%s.%s.%s",
                region.getName(),
                environmentName,
                baseDomainName);

        return StringUtils.isBlank(originDomainNameOverride) ?
                defaultOriginDomainName : originDomainNameOverride;
    }
}
