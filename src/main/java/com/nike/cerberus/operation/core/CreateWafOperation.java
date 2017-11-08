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
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.LoadBalancerParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.WafParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateWafOperation implements Operation<CreateWafCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateWafOperation(final EnvironmentMetadata environmentMetadata,
                              final CloudFormationService cloudFormationService,
                              final ConfigStore configStore,
                              @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateWafCommand command) {
        final String environmentName = environmentMetadata.getName();

        final WafParameters wafParameters = new WafParameters()
                .setLoadBalancerStackName(StackName.LOAD_BALANCER.getFullName(environmentName));

        wafParameters.getTagParameters().setTagEmail(command.getTagsDelegate().getTagEmail());
        wafParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentName);
        wafParameters.getTagParameters().setTagCostcenter(command.getTagsDelegate().getTagCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(wafParameters, typeReference);

        cloudFormationService.createStack(StackName.WAF.getFullName(environmentName),
                parameters, ConfigConstants.WAF_STACK_TEMPLATE_PATH, true);    }

    @Override
    public boolean isRunnable(final CreateWafCommand command) {
        try {
            String environmentName = environmentMetadata.getName();
            cloudFormationService.getStackId(StackName.LOAD_BALANCER.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("The load balancer stack must exist to create the WAF!", iae);
        }

        return true;
    }
}
