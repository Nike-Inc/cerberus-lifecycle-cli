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

package com.nike.cerberus.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.EnterStandbyRequest;
import com.amazonaws.services.autoscaling.model.ExitStandbyRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.beust.jcommander.internal.Lists;
import com.nike.cerberus.store.ConfigStore;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with the EC2 AutoScaling API.
 */
public class AutoScalingService {

    private final AwsClientFactory<AmazonAutoScalingClient> autoScalingClientFactory;
    private final AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;
    private final ConfigStore configStore;

    @Inject
    public AutoScalingService(AwsClientFactory<AmazonAutoScalingClient> autoScalingClientFactory,
                              AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory,
                              ConfigStore configStore) {

        this.autoScalingClientFactory = autoScalingClientFactory;
        this.amazonEC2ClientFactory = amazonEC2ClientFactory;
        this.configStore = configStore;
    }

    /**
     * For a given AutoScaling group logical id, get the public dns names associated with each instance in the
     * primary region
     *
     * @param logicalId AutoScaling group logical id
     * @return List of public dns names
     */
    public List<String> getPublicDnsForAutoScalingGroup(String logicalId) {
        return getPublicDnsForAutoScalingGroup(configStore.getPrimaryRegion(), logicalId);
    }

    /**
     * For a given AutoScaling group logical id, get the public dns names associated with each instance in the
     * provided region
     *
     * @param region The region to use
     * @param logicalId AutoScaling group logical id
     * @return List of public dns names
     */
    public List<String> getPublicDnsForAutoScalingGroup(Regions region, String logicalId) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);

        List<String> instanceIds = Lists.newLinkedList();
        Optional<AutoScalingGroup> autoScalingGroup = describeAutoScalingGroup(region, logicalId);
        List<String> publicDnsNames = Lists.newLinkedList();

        if (autoScalingGroup.isPresent()) {
            autoScalingGroup.get()
                    .getInstances().stream().forEach(instance -> instanceIds.add(instance.getInstanceId()));

            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
                    .withInstanceIds(instanceIds);
            DescribeInstancesResult describeInstancesResult =
                    ec2Client.describeInstances(describeInstancesRequest);

            describeInstancesResult.getReservations().forEach(reservation ->
                    reservation.getInstances().stream().forEach(instance ->
                            publicDnsNames.add(instance.getPublicDnsName()))
            );
        }

        return publicDnsNames;
    }

    /**
     * Updates the minimum number of instances allowed in the auto scaling group in the primary region
     *
     * @param logicalId - Name of the auto scaling group
     */
    public void updateMinInstancesForAutoScalingGroup(String logicalId, int minInstances) {
        updateMinInstancesForAutoScalingGroup(configStore.getPrimaryRegion(), logicalId, minInstances);
    }

    /**
     * Updates the minimum number of instances allowed in the auto scaling group
     *
     * @param region The region to use
     * @param logicalId - Name of the auto scaling group
     */
    public void updateMinInstancesForAutoScalingGroup(Regions region, String logicalId, int minInstances) {
        AmazonAutoScalingClient autoScalingClient = autoScalingClientFactory.getClient(region);

        UpdateAutoScalingGroupRequest request = new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(logicalId)
                .withMinSize(minInstances);

        autoScalingClient.updateAutoScalingGroup(request);
    }

    /**
     * Set an EC2 instance to standby state, so that the desired instance count on the AutoScaling group is decreased
     * and a new instance is not spun up on instance reboot. This also removes the instance from the ELB, so that the
     * instance is not terminated when the health check fails.
     *
     * uses the primary region
     *
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToStandby(String logicalId, String instanceId) {
        setInstanceStateToStandby(configStore.getPrimaryRegion(), logicalId, instanceId);
    }

    /**
     * Set an EC2 instance to standby state, so that the desired instance count on the AutoScaling group is decreased
     * and a new instance is not spun up on instance reboot. This also removes the instance from the ELB, so that the
     * instance is not terminated when the health check fails.
     *
     * @param region The region to use
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToStandby(Regions region, String logicalId, String instanceId) {
        AmazonAutoScalingClient autoScalingClient = autoScalingClientFactory.getClient(region);

        EnterStandbyRequest request = new EnterStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId)
                .withShouldDecrementDesiredCapacity(true);

        autoScalingClient.enterStandby(request);
    }

    /**
     * Signify that the EC2 instance is now in service and ready to be re-added to the ELB and AutoScaling group. This
     * will also increase the desired instance count for the ASG in the primary region.
     *
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToInService(String logicalId, String instanceId) {
        setInstanceStateToInService(configStore.getPrimaryRegion(), logicalId, instanceId);
    }

    /**
     * Signify that the EC2 instance is now in service and ready to be re-added to the ELB and AutoScaling group. This
     * will also increase the desired instance count for the ASG in the provided region.
     *
     * @param region The region to use
     * @param logicalId  - Name of the auto scaling group
     * @param instanceId - ID of the EC2 instance
     */
    public void setInstanceStateToInService(Regions region, String logicalId, String instanceId) {

        AmazonAutoScalingClient autoScalingClient = autoScalingClientFactory.getClient(region);

        ExitStandbyRequest request = new ExitStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId);

        autoScalingClient.exitStandby(request);
    }

    /**
     *
     * @param region The region to look for the ASG
     * @param autoScalingGroupName The ASG name to look for.
     * @return AutoScalingGroup details
     */
    private Optional<AutoScalingGroup> describeAutoScalingGroup(Regions region, String autoScalingGroupName) {
        AmazonAutoScalingClient autoScalingClient = autoScalingClientFactory.getClient(region);

        DescribeAutoScalingGroupsRequest describeAsg = new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(autoScalingGroupName);
        DescribeAutoScalingGroupsResult result = autoScalingClient.describeAutoScalingGroups(describeAsg);

        return result.getAutoScalingGroups().stream().findFirst();
    }
}
