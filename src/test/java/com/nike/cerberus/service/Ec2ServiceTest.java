package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.AvailabilityZoneState;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairResult;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Ec2ServiceTest {

    @Test
    public void testImportKey() {

        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

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
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

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
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

        String keyName = "key-name";

        when(ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyName)))
                .thenReturn(new DescribeKeyPairsResult());

        // invoke method under test
        assertFalse(ec2Service.isKeyPairPresent(keyName));
    }

    @Test
    public void testIsKeyPairPresentFalseNotFound() {
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

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
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

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
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        Ec2Service ec2Service = new Ec2Service(ec2Client);

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

}