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
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.LoadBalancerParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
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

        final String sslCertificateArn = configStore.getServerCertificateArn(Stack.CMS)
                .orElseThrow(() -> new IllegalStateException("Could not retrieve SSL certificate ARN!"));

        final LoadBalancerParameters loadBalancerParameters = new LoadBalancerParameters()
                .setVpcId(vpcOutputs.getVpcId())
                .setSslCertificateArn(sslCertificateArn)
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName))
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3());

        if (StringUtils.isNotBlank(command.getLoadBalancerSslPolicyOverride())) {
            loadBalancerParameters.setSslPolicy(command.getLoadBalancerSslPolicyOverride());
        }

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {
        };
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(loadBalancerParameters, typeReference);

        final String stackId = cloudFormationService.createStack(Stack.LOAD_BALANCER, parameters, true,
                command.getTagsDelegate().getTags());

        StackStatus endStatus = cloudFormationService.waitForStatus(stackId,
                Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (endStatus != StackStatus.CREATE_COMPLETE) {
            throw new UnexpectedCloudFormationStatusException(String.format("Unexpected end status: %s", endStatus.name()));
        }
    }

    @Override
    public boolean isRunnable(final CreateLoadBalancerCommand command) {
        String environmentName = environmentMetadata.getName();
        try {
            cloudFormationService.getStackId(Stack.SECURITY_GROUPS.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("The security group stack must exist to create the load balancer!");
            return false;
        }

        return !cloudFormationService.isStackPresent(Stack.LOAD_BALANCER.getFullName(environmentName));
    }
}
