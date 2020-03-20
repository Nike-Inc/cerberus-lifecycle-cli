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
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.domain.cloudformation.WafParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateWafOperation implements Operation<CreateWafCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final ConfigStore configStore;

    @Inject
    public CreateWafOperation(@Named(ENV_NAME) String environmentName,
                              CloudFormationService cloudFormationService,
                              CloudFormationObjectMapper cloudFormationObjectMapper,
                              ConfigStore configStore) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.configStore = configStore;
    }

    @Override
    public void run(CreateWafCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());
        WafParameters wafParameters = new WafParameters()
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName))
                .setWafName("cerberus-" + environmentName + "-waf");

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(wafParameters);

        cloudFormationService.createStackAndWait(
                region,
                Stack.WAF,
                parameters,
                true,
                command.getCloudFormationParametersDelegate().getTags()
        );
    }

    @Override
    public boolean isRunnable(CreateWafCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());
        try {
            cloudFormationService.getStackId(region, Stack.LOAD_BALANCER.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("The load balancer stack must exist to create the WAF!");
            return false;
        }

        return !cloudFormationService.isStackPresent(region,
                Stack.ROUTE53.getFullName(environmentName));
    }
}
