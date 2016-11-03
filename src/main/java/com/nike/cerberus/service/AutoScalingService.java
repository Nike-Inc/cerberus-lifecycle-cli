package com.nike.cerberus.service;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
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
        final DescribeAutoScalingGroupsRequest describeAsg = new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(logicalId);
        final DescribeAutoScalingGroupsResult result = autoScalingClient.describeAutoScalingGroups(describeAsg);
        final Optional<AutoScalingGroup> autoScalingGroup = result.getAutoScalingGroups().stream().findFirst();
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
}
