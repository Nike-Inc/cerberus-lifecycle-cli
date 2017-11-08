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
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.LoadBalancerParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
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
public class CreateLoadBalancerOperation implements Operation<CreateLoadBalancerCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateLoadBalancerOperation(final EnvironmentMetadata environmentMetadata,
                              final CloudFormationService cloudFormationService,
                              final ConfigStore configStore,
                              @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateLoadBalancerCommand command) {
        final String environmentName = environmentMetadata.getName();
        final VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();

        final String sslCertificateArn = configStore.getServerCertificateArn(StackName.CMS)
                .orElseThrow(() -> new IllegalStateException("Could not retrieve SSL certificate ARN!"));

        final LoadBalancerParameters loadBalancerParameters = new LoadBalancerParameters()
                .setVpcId(vpcOutputs.getVpcId())
                .setSslCertificateArn(sslCertificateArn)
                .setSgStackName(StackName.SECURITY_GROUPS.getFullName(environmentName))
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3());

        loadBalancerParameters.getTagParameters().setTagEmail(command.getTagsDelegate().getTagEmail());
        loadBalancerParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentName);
        loadBalancerParameters.getTagParameters().setTagCostcenter(command.getTagsDelegate().getTagCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(loadBalancerParameters, typeReference);

        cloudFormationService.createStack(StackName.LOAD_BALANCER.getFullName(environmentName),
                parameters, ConfigConstants.LOAD_BALANCER_STACK_TEMPLATE_PATH, true);    }

    @Override
    public boolean isRunnable(final CreateLoadBalancerCommand command) {
        try {
            String environmentName = environmentMetadata.getName();
            cloudFormationService.getStackId(StackName.SECURITY_GROUPS.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("The security group stack must exist to create the load balancer!", iae);
        }

        return true;
    }
}
