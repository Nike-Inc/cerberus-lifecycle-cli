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

package com.nike.cerberus.service;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.EnterStandbyRequest;
import com.amazonaws.services.autoscaling.model.ExitStandbyRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.beust.jcommander.internal.Lists;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with the EC2 AutoScaling API.
 */
public class AutoScalingService {

    private final AmazonAutoScaling autoScalingClient;

    private final AmazonEC2 ec2Client;

    @Inject
    public AutoScalingService(final AmazonAutoScaling autoScalingClient, final AmazonEC2 ec2Client) {
        this.autoScalingClient = autoScalingClient;
        this.ec2Client = ec2Client;
    }

    /**
     * For a given AutoScaling group logical id, get the public dns names associated with each instance.
     *
     * @param logicalId AutoScaling group logical id
     * @return List of public dns names
     */
    public List<String> getPublicDnsForAutoScalingGroup(final String logicalId) {
        final List<String> instanceIds = Lists.newLinkedList();
        final Optional<AutoScalingGroup> autoScalingGroup = describeAutoScalingGroup(logicalId);
        final List<String> publicDnsNames = Lists.newLinkedList();

        if (autoScalingGroup.isPresent()) {
            autoScalingGroup.get()
                    .getInstances().stream().forEach(instance -> instanceIds.add(instance.getInstanceId()));

            final DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
                    .withInstanceIds(instanceIds);
            final DescribeInstancesResult describeInstancesResult =
                    ec2Client.describeInstances(describeInstancesRequest);

            describeInstancesResult.getReservations().forEach(reservation ->
                    reservation.getInstances().stream().forEach(instance ->
                            publicDnsNames.add(instance.getPublicDnsName()))
            );
        }

        return publicDnsNames;
    }

    /**
     * Updates the minimum number of instances allowed in the auto scaling group
     *
     * @param logicalId - Name of the auto scaling group
     */
    public void updateMinInstancesForAutoScalingGroup(final String logicalId, final int minInstances) {

        final UpdateAutoScalingGroupRequest request = new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(logicalId)
                .withMinSize(minInstances);

        autoScalingClient.updateAutoScalingGroup(request);
    }

    /**
     * Set an EC2 instance to standby state, so that the desired instance count on the AutoScaling group is decreased
     * and a new instance is not spun up on instance reboot. This also removes the instance from the ELB, so that the
     * instance is not terminated when the health check fails.
     *
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToStandby(final String logicalId, final String instanceId) {
        final EnterStandbyRequest request = new EnterStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId)
                .withShouldDecrementDesiredCapacity(true);

        autoScalingClient.enterStandby(request);
    }

    /**
     * Signify that the EC2 instance is now in service and ready to be re-added to the ELB and AutoScaling group. This
     * will also increase the desired instance count for the ASG.
     *
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToInService(final String logicalId, final String instanceId) {
        final ExitStandbyRequest request = new ExitStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId);

        autoScalingClient.exitStandby(request);
    }

    private Optional<AutoScalingGroup> describeAutoScalingGroup(final String autoscalingGroupName) {
        final DescribeAutoScalingGroupsRequest describeAsg = new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(autoscalingGroupName);
        final DescribeAutoScalingGroupsResult result = autoScalingClient.describeAutoScalingGroups(describeAsg);

        return result.getAutoScalingGroups().stream().findFirst();
    }
}
