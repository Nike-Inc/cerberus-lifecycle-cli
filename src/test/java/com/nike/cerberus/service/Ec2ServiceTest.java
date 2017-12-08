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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.AvailabilityZoneState;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.nike.cerberus.store.ConfigStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.nike.cerberus.service.Ec2Service.FILTER_NAME_TEMPL_FOR_EC2_TAGS;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_FILTER_NAME;
import static com.nike.cerberus.service.Ec2Service.INSTANCE_STATE_RUNNING_FILTER_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class Ec2ServiceTest {

    @Mock
    ConfigStore configStore;

    @Mock
    AmazonEC2Client ec2Client;

    @Mock
    AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;

    private Ec2Service ec2Service;

    @Before
    public void setup() {
        initMocks(this);
        when(amazonEC2ClientFactory.getClient(any())).thenReturn(ec2Client);
        ec2Service = new Ec2Service(amazonEC2ClientFactory, configStore);
    }

    @Test
    public void testImportKey() {

        String keyName = "key-name";
        String publicKeyMaterial = "public-key-material";
        String keyNameResult = "key-name-result";

        when(ec2Client.importKeyPair(new ImportKeyPairRequest(keyName, publicKeyMaterial)))
                .thenReturn(new ImportKeyPairResult().withKeyName(keyNameResult));

        // invoke method under test
        String result = ec2Service.importKey(keyName, publicKeyMaterial);

        assertEquals(keyNameResult, result);
    }

    @Test
    public void testIsKeyPairPresentTrue() {

        String keyName = "key-name";

        when(ec2Client.describeKeyPairs(
                new DescribeKeyPairsRequest()
                        .withKeyNames(keyName)
                )
        ).thenReturn(
                new DescribeKeyPairsResult()
                        .withKeyPairs(
                                new KeyPairInfo()
                        )
        );

        // invoke method under test
        assertTrue(ec2Service.isKeyPairPresent(keyName));
    }

    @Test
    public void testIsKeyPairPresentFalse() {

        String keyName = "key-name";

        when(ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyName)))
                .thenReturn(new DescribeKeyPairsResult());

        // invoke method under test
        assertFalse(ec2Service.isKeyPairPresent(keyName));
    }

    @Test
    public void testIsKeyPairPresentFalseNotFound() {

        String keyName = "key-name";

        AmazonServiceException ex = new AmazonServiceException("fake-exception");
        ex.setErrorCode("InvalidKeyPair.NotFound");

        when(ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyName)))
                .thenThrow(ex);

        // invoke method under test
        assertFalse(ec2Service.isKeyPairPresent(keyName));
    }

    @Test
    public void testIsKeyPairPresentException() {

        String keyName = "key-name";
        String fakeExceptionMessage = "fake-exception";

        when(ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyName)))
                .thenThrow(new AmazonServiceException(fakeExceptionMessage));

        try {
            // invoke method under test
            ec2Service.isKeyPairPresent(keyName);
            fail("expected exception not passed up");
        } catch (AmazonServiceException ex) {
            // pass
            assertEquals(fakeExceptionMessage, ex.getErrorMessage());
        }
    }

    @Test
    public void testGetAvailabilityZones() {

        String zoneName = "zone-name";

        when(ec2Client.describeAvailabilityZones()).thenReturn(
                new DescribeAvailabilityZonesResult()
                        .withAvailabilityZones(
                                new AvailabilityZone()
                                        .withZoneName(zoneName)
                                        .withState(AvailabilityZoneState.Available),
                                new AvailabilityZone()
                                        .withZoneName("not-available-zone")
                                        .withState(AvailabilityZoneState.Unavailable)
                        )
        );

        // invoke method under test
        List<String> results = ec2Service.getAvailabilityZones();

        assertEquals(1, results.size());
        assertEquals(zoneName, results.get(0));
    }

    @Test
    public void testGetInstancesByTagHappy() {

        String tagKey = "tag key";
        String tagValue = "tag value";
        Filter filter = new Filter().withName(INSTANCE_STATE_FILTER_NAME).withValues(INSTANCE_STATE_RUNNING_FILTER_VALUE);
        Instance instance = mock(Instance.class);

        when(ec2Client.describeInstances(new DescribeInstancesRequest()
                .withFilters(
                        filter,
                        new Filter()
                                .withName(String.format(FILTER_NAME_TEMPL_FOR_EC2_TAGS, tagKey))
                                .withValues(tagValue)
                )
        )).thenReturn(
                new DescribeInstancesResult()
                        .withReservations(
                                new Reservation()
                                        .withInstances(instance))
        );

        List<Instance> instances = ec2Service.getInstancesByTag(tagKey, tagValue, filter);

        assertTrue(instances.contains(instance));
    }

    @Test
    public void testRebootInstancesHappy() {

        String instanceId = "instance id";

        ec2Service.rebootEc2Instance(instanceId);

        verify(ec2Client).rebootInstances(new RebootInstancesRequest()
                .withInstanceIds(instanceId)
        );
    }

}