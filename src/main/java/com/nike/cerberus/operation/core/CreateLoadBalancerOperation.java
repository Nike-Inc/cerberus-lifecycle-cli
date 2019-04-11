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
import com.google.common.collect.ImmutableMap;
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
import static java.util.stream.Collectors.joining;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateLoadBalancerOperation implements Operation<CreateLoadBalancerCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
    private final Map<String, String> regionToAwsElbAccountIdMap = ImmutableMap.<String, String>builder()
            .put("us-east-1",      "127311923021")
            .put("us-east-2",      "033677994240")
            .put("us-west-1",      "027434742980")
            .put("us-west-2",      "797873946194")
            .put("ca-central-1",   "985666609251")
            .put("eu-central-1",   "054676820928")
            .put("eu-west-1",      "156460612806")
            .put("eu-west-2",      "652711504416")
            .put("eu-west-3",      "009996457667")
            .put("ap-northeast-1", "582318560864")
            .put("ap-northeast-2", "600734575887")
            .put("ap-southeast-1", "114774131450")
            .put("ap-southeast-2", "783225319266")
            .put("ap-south-1",     "718504428378")
            .put("sa-east-1",      "507241528517")
            .put("us-gov-west-1",  "048591011584")
            .put("cn-north-1",     "638102146993")
            .put("cn-northwest-1", "037604701340")
            .build();

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
        Regions loadBalancerRegion = configStore.getPrimaryRegion();

        VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();

        // Use latest cert, if there happens to be more than one for some reason
        String sslCertificateArn = configStore.getCertificationInformationList()
                .getLast().getIdentityManagementCertificateArn();

        if (!regionToAwsElbAccountIdMap.containsKey(loadBalancerRegion.getName())) {
            throw new RuntimeException(
                    String.format("The region: %s was not in the region to AWS ELB Account Id map: [ %s ]",
                            loadBalancerRegion.getName(),
                            regionToAwsElbAccountIdMap.entrySet().stream()
                                    .map(entry -> entry.getKey() + "->" + entry.getValue())
                                    .collect(joining(", ")))
            );
        }

        LoadBalancerParameters loadBalancerParameters = new LoadBalancerParameters()
                .setVpcId(vpcOutputs.getVpcId())
                .setSslCertificateArn(sslCertificateArn)
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName))
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3())
                .setElasticLoadBalancingAccountId(regionToAwsElbAccountIdMap.get(loadBalancerRegion.getName()));


        if (StringUtils.isNotBlank(command.getLoadBalancerSslPolicyOverride())) {
            loadBalancerParameters.setSslPolicy(command.getLoadBalancerSslPolicyOverride());
        }

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(loadBalancerParameters);

        cloudFormationService.createStackAndWait(
                loadBalancerRegion,
                Stack.LOAD_BALANCER,
                parameters,
                true,
                command.getCloudFormationParametersDelegate().getTags());
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
