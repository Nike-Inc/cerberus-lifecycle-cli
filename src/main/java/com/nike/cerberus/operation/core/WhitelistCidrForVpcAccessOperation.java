/*
 * Copyright (c) 2016 Nike, Inc.
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

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.google.common.collect.Lists;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.DatabaseOutputs;
import com.nike.cerberus.domain.cloudformation.SecurityGroupOutputs;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Operation
 */
public class WhitelistCidrForVpcAccessOperation implements Operation<WhitelistCidrForVpcAccessCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final AmazonEC2 ec2Client;

    @Inject
    public WhitelistCidrForVpcAccessOperation(final CloudFormationService cloudFormationService,
                                              final ConfigStore configStore,
                                              final AmazonEC2 ec2Client) {
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.ec2Client = ec2Client;
    }

    @Override
    public void run(final WhitelistCidrForVpcAccessCommand command) {
        final SecurityGroupOutputs securityGroupOutputs = configStore.getSecurityGroupStackOutputs();

        logger.info("Revoking the previous ingress rules...");
        final DescribeSecurityGroupsResult securityGroupsResult = ec2Client.describeSecurityGroups(
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
        final List<IpPermission> ipPermissionList = Lists.newArrayListWithCapacity(command.getPorts().size());
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
    public boolean isRunnable(final WhitelistCidrForVpcAccessCommand command) {
        boolean isRunnable = true;
        final String baseStackId = configStore.getStackId(StackName.BASE);

        if (StringUtils.isBlank(baseStackId) || !cloudFormationService.isStackPresent(baseStackId)) {
            logger.error("No base stack defined for this environment!");
            isRunnable = false;
        }

        return isRunnable;
    }
}
