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

package com.nike.cerberus.command.core;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

/**
 * Command for granting CIDRs ingress to specific ports within the Cerberus VPC.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the base components to support Cerberus.")
public class WhitelistCidrForVpcAccessCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "whitelist-cidr-for-vpc-access";

    public static final String CIDR_LONG_ARG = "-cidr";

    public static final String PORT_LONG_ARG = "-port";

    @Parameter(names = CIDR_LONG_ARG, description = "One or more CIDRs to be granted ingress on the Cerberus VPC.")
    private List<String> cidrs = new ArrayList<>();

    @Parameter(names = PORT_LONG_ARG, description = "The ports to grant ingress on within the Cerberus VPC.")
    private List<Integer> ports = new ArrayList<>();

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final AmazonEC2 ec2Client;

    @Inject
    public WhitelistCidrForVpcAccessCommand(final CloudFormationService cloudFormationService,
                                              final ConfigStore configStore,
                                              final AmazonEC2 ec2Client) {
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.ec2Client = ec2Client;
    }

    @Override
    public void execute() {
        final BaseOutputs baseStackOutputs = configStore.getBaseStackOutputs();

        logger.info("Revoking the previous ingress rules...");
        final DescribeSecurityGroupsResult securityGroupsResult = ec2Client.describeSecurityGroups(
                new DescribeSecurityGroupsRequest().withGroupIds(baseStackOutputs.getToolsIngressSgId()));
        securityGroupsResult.getSecurityGroups().forEach(securityGroup -> {
            if (!securityGroup.getIpPermissions().isEmpty()) {
                RevokeSecurityGroupIngressRequest revokeIngressRequest = new RevokeSecurityGroupIngressRequest()
                        .withGroupId(baseStackOutputs.getToolsIngressSgId())
                        .withIpPermissions(securityGroup.getIpPermissions());
                ec2Client.revokeSecurityGroupIngress(revokeIngressRequest);
            }
        });
        logger.info("Done.");

        logger.info("Authorizing the new ingress rules...");
        final List<IpPermission> ipPermissionList = Lists.newArrayListWithCapacity(ports.size());
        ports.forEach(port -> {
            IpPermission ipPermission = new IpPermission()
                    .withIpRanges(cidrs)
                    .withIpProtocol("tcp")
                    .withFromPort(port)
                    .withToPort(port);

            ipPermissionList.add(ipPermission);
        });

        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(baseStackOutputs.getToolsIngressSgId())
                .withIpPermissions(ipPermissionList);
        ec2Client.authorizeSecurityGroupIngress(ingressRequest);
        logger.info("Done.");
    }

    @Override
    public boolean isRunnable() {
        boolean isRunnable = true;
        final String baseStackId = configStore.getStackId(StackName.BASE);

        if (StringUtils.isBlank(baseStackId) || !cloudFormationService.isStackPresent(baseStackId)) {
            logger.error("No base stack defined for this environment!");
            isRunnable = false;
        }

        return isRunnable;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
}
