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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.google.common.collect.Lists;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.cloudformation.SecurityGroupOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation
 */
public class WhitelistCidrForVpcAccessOperation implements Operation<WhitelistCidrForVpcAccessCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final AwsClientFactory<AmazonEC2Client> amazonS3ClientFactory;

    private final String environmentName;

    @Inject
    public WhitelistCidrForVpcAccessOperation(CloudFormationService cloudFormationService,
                                              ConfigStore configStore,
                                              AwsClientFactory<AmazonEC2Client> amazonS3ClientFactory,
                                              @Named(ENV_NAME) String environmentName) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.amazonS3ClientFactory = amazonS3ClientFactory;
        this.environmentName = environmentName;
    }

    @Override
    public void run(WhitelistCidrForVpcAccessCommand command) {
        Regions region = command.getStackRegion().orElse(configStore.getPrimaryRegion());
        AmazonEC2Client ec2Client = amazonS3ClientFactory.getClient(region);

        SecurityGroupOutputs securityGroupOutputs = configStore.getSecurityGroupStackOutputs(region);

        logger.info("Revoking the previous ingress rules...");
        DescribeSecurityGroupsResult securityGroupsResult = ec2Client.describeSecurityGroups(
                new DescribeSecurityGroupsRequest().withGroupIds(securityGroupOutputs.getWhitelistIngressSgId()));
        securityGroupsResult.getSecurityGroups().forEach(securityGroup -> {
            if (!securityGroup.getIpPermissions().isEmpty()) {
                RevokeSecurityGroupIngressRequest revokeIngressRequest = new RevokeSecurityGroupIngressRequest()
                        .withGroupId(securityGroupOutputs.getWhitelistIngressSgId())
                        .withIpPermissions(securityGroup.getIpPermissions());
                ec2Client.revokeSecurityGroupIngress(revokeIngressRequest);
            }
        });
        logger.info("Done.");

        logger.info("Authorizing the new ingress rules...");
        List<IpPermission> ipPermissionList = Lists.newArrayListWithCapacity(command.getPorts().size());
        command.getPorts().forEach(port -> {
            IpPermission ipPermission = new IpPermission()
                    .withIpRanges(command.getCidrs())
                    .withIpProtocol("tcp")
                    .withFromPort(port)
                    .withToPort(port);

            ipPermissionList.add(ipPermission);
        });

        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(securityGroupOutputs.getWhitelistIngressSgId())
                .withIpPermissions(ipPermissionList);
        ec2Client.authorizeSecurityGroupIngress(ingressRequest);
        logger.info("Done.");
    }

    @Override
    public boolean isRunnable(WhitelistCidrForVpcAccessCommand command) {
        Regions region = command.getStackRegion().orElse(configStore.getPrimaryRegion());
        try {
            cloudFormationService.getStackId(region, Stack.SECURITY_GROUPS.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("The security group stack must exist before you can white list ingress to the VPC Ingress SG.", iae);
            return false;
        }

        return true;
    }
}
