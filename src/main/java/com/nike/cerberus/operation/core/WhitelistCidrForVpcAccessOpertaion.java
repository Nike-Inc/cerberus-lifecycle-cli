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
public class WhitelistCidrForVpcAccessOpertaion implements Operation<WhitelistCidrForVpcAccessCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final AmazonEC2 ec2Client;

    @Inject
    public WhitelistCidrForVpcAccessOpertaion(final CloudFormationService cloudFormationService,
                                              final ConfigStore configStore,
                                              final AmazonEC2 ec2Client) {
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.ec2Client = ec2Client;
    }

    @Override
    public void run(final WhitelistCidrForVpcAccessCommand command) {
        final BaseOutputs baseStackOutputs = configStore.getBaseStackOutputs();
        final List<IpPermission> ipPermissionList = Lists.newArrayListWithCapacity(command.getPorts().size());

        logger.info("Building IP permission list...");
        command.getPorts().forEach(port -> {
            IpPermission ipPermission = new IpPermission()
                    .withIpRanges(command.getCidrs())
                    .withIpProtocol("tcp")
                    .withFromPort(port)
                    .withToPort(port);

            ipPermissionList.add(ipPermission);
        });

        logger.info("Sending revoke previous ingress rules request...");
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

        logger.info("Sending authorize ingress rules request...");
        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(baseStackOutputs.getToolsIngressSgId())
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
