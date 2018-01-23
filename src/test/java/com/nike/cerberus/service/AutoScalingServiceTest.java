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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.EnterStandbyRequest;
import com.amazonaws.services.autoscaling.model.ExitStandbyRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.nike.cerberus.store.ConfigStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AutoScalingServiceTest {

    @Mock
    private ConfigStore configStore;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AmazonEC2Client ec2Client;

    @Mock
    private AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;

    @Mock
    private AwsClientFactory<AmazonAutoScalingClient> amazonAutoScalingClientFactory;

    private AutoScalingService autoScalingService;

    @Before
    public void setup() {
        initMocks(this);
        when(amazonEC2ClientFactory.getClient(any())).thenReturn(ec2Client);
        when(amazonAutoScalingClientFactory.getClient(any())).thenReturn(autoScalingClient);

        autoScalingService = new AutoScalingService(amazonAutoScalingClientFactory, amazonEC2ClientFactory, configStore);
    }

    @Test
    public void testGetPublicDnsForAutoScalingGroup() {

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

    @Test
    public void testIncrementMinInstancesForAsgHappy() {

        String logicalId = "asg id";
        String instanceId = "instance id";
        int minSize = 2;

        when(autoScalingClient.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(logicalId))
        ).thenReturn(
                new DescribeAutoScalingGroupsResult()
                        .withAutoScalingGroups(
                                new AutoScalingGroup().withInstances(
                                        new Instance().withInstanceId(instanceId))
                                        .withMinSize(minSize)
                        )
        );

        autoScalingService.updateMinInstancesForAutoScalingGroup(logicalId, minSize - 1);

        verify(autoScalingClient).updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(logicalId)
                .withMinSize(minSize - 1));
    }

    @Test
    public void testSetInstanceStateToInServiceHappy() {

        String logicalId = "asg id";
        String instanceId = "instance id";

        autoScalingService.setInstanceStateToInService(logicalId, instanceId);

        verify(autoScalingClient).exitStandby(new ExitStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId)
        );
    }

    @Test
    public void testSetInstanceStateToStandbyHappy() {

        String logicalId = "asg id";
        String instanceId = "instance id";

        autoScalingService.setInstanceStateToStandby(logicalId, instanceId);

        verify(autoScalingClient).enterStandby(new EnterStandbyRequest()
                .withAutoScalingGroupName(logicalId)
                .withInstanceIds(instanceId)
                .withShouldDecrementDesiredCapacity(true)
        );
    }

}