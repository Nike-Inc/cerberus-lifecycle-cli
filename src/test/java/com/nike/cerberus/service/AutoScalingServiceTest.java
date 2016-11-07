/*
 * Copyright (c) 2016 Nike Inc.
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
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoScalingServiceTest {

    @Test
    public void testGetPublicDnsForAutoScalingGroup() {

        AmazonAutoScaling autoScalingClient = mock(AmazonAutoScaling.class);
        AmazonEC2 ec2Client = mock(AmazonEC2.class);

        AutoScalingService autoScalingService = new AutoScalingService(autoScalingClient, ec2Client);

        String logicalId = "fake-logical-id";
        String instanceId = "fake-instance-id";
        String dnsName = "dns.name";

        when(autoScalingClient.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(logicalId)
                )
        ).thenReturn(
                new DescribeAutoScalingGroupsResult()
                        .withAutoScalingGroups(
                                new AutoScalingGroup()
                                        .withInstances(
                                                new Instance()
                                                        .withInstanceId(instanceId)
                                        )
                        )
        );

        when(ec2Client.describeInstances(
                new DescribeInstancesRequest()
                        .withInstanceIds(instanceId)
                )
        ).thenReturn(
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation()
                                        .withInstances(
                                                new com.amazonaws.services.ec2.model.Instance()
                                                        .withPublicDnsName(dnsName)
                                        )
                        )
        );

        // invoke method under test
        List<String> results = autoScalingService.getPublicDnsForAutoScalingGroup(logicalId);

        assertEquals(1, results.size());
        assertEquals(dnsName, results.get(0));
    }


    @Test
    public void testGetPublicDnsForAutoScalingGroupNoInstancesFound() {

        AmazonAutoScaling autoScalingClient = mock(AmazonAutoScaling.class);
        AmazonEC2 ec2Client = mock(AmazonEC2.class);

        AutoScalingService autoScalingService = new AutoScalingService(autoScalingClient, ec2Client);

        String logicalId = "fake-logical-id";

        when(autoScalingClient.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(logicalId)
                )
        ).thenReturn(
                new DescribeAutoScalingGroupsResult()
        );

        // invoke method under test
        List<String> results = autoScalingService.getPublicDnsForAutoScalingGroup(logicalId);

        assertEquals(0, results.size());
    }

}