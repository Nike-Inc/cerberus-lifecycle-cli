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

import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.domain.cloudformation.LoadBalancerParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
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
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateLoadBalancerOperation implements Operation<CreateLoadBalancerCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateLoadBalancerOperation(@Named(ENV_NAME) String environmentName,
                                       CloudFormationService cloudFormationService,
                                       ConfigStore configStore,
                                       CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(CreateLoadBalancerCommand command) {
        VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();

        // Use latest cert, if there happens to be more than one for some reason
        String sslCertificateArn = configStore.getCertificationInformationList()
                .getLast().getIdentityManagementCertificateArn();

        LoadBalancerParameters loadBalancerParameters = new LoadBalancerParameters()
                .setVpcId(vpcOutputs.getVpcId())
                .setSslCertificateArn(sslCertificateArn)
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName))
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3());

        if (StringUtils.isNotBlank(command.getLoadBalancerSslPolicyOverride())) {
            loadBalancerParameters.setSslPolicy(command.getLoadBalancerSslPolicyOverride());
        }

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(loadBalancerParameters);

        cloudFormationService.createStackAndWait(
                configStore.getPrimaryRegion(),
                Stack.LOAD_BALANCER,
                parameters,
                true,
                command.getTagsDelegate().getTags());
    }

    @Override
    public boolean isRunnable(CreateLoadBalancerCommand command) {
        boolean isRunnable = true;

        if (!cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.SECURITY_GROUPS.getFullName(environmentName))) {
            logger.error("The security group stack must exist to create the load balancer!");
            isRunnable = false;
        }

        if (configStore.getCertificationInformationList().isEmpty()) {
            logger.error("TLS Certificate files have not been uploaded for environment");
            isRunnable = false;
        }

        if (cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.LOAD_BALANCER.getFullName(environmentName))) {
            logger.error("The load balancer stack already exists");
            isRunnable = false;
        }

        return isRunnable;
    }
}
