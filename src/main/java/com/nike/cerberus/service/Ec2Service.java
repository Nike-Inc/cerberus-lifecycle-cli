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

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service wrapper for AWS EC2.
 */
public class Ec2Service {

    private final AmazonEC2 ec2Client;

    @Inject
    public Ec2Service(final AmazonEC2 ec2Client) {
        this.ec2Client = ec2Client;
    }

    /**
     * Import a key pair to AWS EC2.
     *
     * @param keyName Friendly name for the key
     * @param publicKeyMaterial Public key
     * @return Key name
     */
    public String importKey(final String keyName, final String publicKeyMaterial) {
        final ImportKeyPairRequest request = new ImportKeyPairRequest(keyName, publicKeyMaterial);
        final ImportKeyPairResult result = ec2Client.importKeyPair(request);
        return result.getKeyName();
    }

    /**
     * Checks if a key pair is present in AWS EC2.
     *
     * @param keyName Friendly name for the key
     * @return If present
     */
    public boolean isKeyPairPresent(final String keyName) {
        final DescribeKeyPairsRequest request = new DescribeKeyPairsRequest().withKeyNames(keyName);

        try {
            final DescribeKeyPairsResult result = ec2Client.describeKeyPairs(request);
            return result.getKeyPairs().size() > 0;
        } catch (final AmazonServiceException ase) {
            if (ase.getErrorCode() == "InvalidKeyPair.NotFound") {
                return false;
            }

            throw ase;
        }
    }

    /**
     * Determines all availabity zones for a region that are marked as available.
     *
     * @return List of availability zones
     */
    public List<String> getAvailabilityZones() {
        final DescribeAvailabilityZonesResult result = ec2Client.describeAvailabilityZones();

        return result.getAvailabilityZones()
                .stream()
                .filter(az -> AvailabilityZoneState.Available == AvailabilityZoneState.fromValue(az.getState()))
                .map(AvailabilityZone::getZoneName).collect(Collectors.toList());
    }
}
