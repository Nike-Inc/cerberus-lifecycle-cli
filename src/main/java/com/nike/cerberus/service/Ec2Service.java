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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
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
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service wrapper for AWS EC2.
 */
public class Ec2Service {

    public final static String EC2_ASG_GROUP_NAME_TAG_KEY = "aws:autoscaling:groupName";

    public final static String INSTANCE_STATE_FILTER_NAME = "instance-state-name";

    public final static String INSTANCE_STATE_RUNNING_FILTER_VALUE = "running";

    protected final static String FILTER_NAME_TEMPL_FOR_EC2_TAGS = "tag:%s";

    private final AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;

    private final ConfigStore configStore;

    @Inject
    public Ec2Service(AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory,
                      ConfigStore configStore) {

        this.amazonEC2ClientFactory = amazonEC2ClientFactory;
        this.configStore = configStore;
    }

    /**
     * Import a key pair to AWS EC2 in the primary region.
     *
     * @param keyName           Friendly name for the key
     * @param publicKeyMaterial Public key
     * @return Key name
     */
    public String importKey(String keyName, String publicKeyMaterial) {
        return importKey(configStore.getPrimaryRegion(), keyName, publicKeyMaterial);
    }

    /**
     * Import a key pair to AWS EC2.
     *
     * @param region            The region to use.
     * @param keyName           Friendly name for the key
     * @param publicKeyMaterial Public key
     * @return Key name
     */
    public String importKey(Regions region, String keyName, String publicKeyMaterial) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);
        ImportKeyPairRequest request = new ImportKeyPairRequest(keyName, publicKeyMaterial);
        ImportKeyPairResult result = ec2Client.importKeyPair(request);
        return result.getKeyName();
    }

    /**
     * Checks if a key pair is present in AWS EC2 in the primary region
     *
     * @param keyName Friendly name for the key
     * @return If present
     */
    public boolean isKeyPairPresent(String keyName) {
        return isKeyPairPresent(configStore.getPrimaryRegion(), keyName);
    }

    /**
     * Checks if a key pair is present in AWS EC2 in the provided region
     *
     * @param region The region to use.
     * @param keyName Friendly name for the key
     * @return If present
     */
    public boolean isKeyPairPresent(Regions region, String keyName) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest().withKeyNames(keyName);

        try {
            DescribeKeyPairsResult result = ec2Client.describeKeyPairs(request);
            return result.getKeyPairs().size() > 0;
        } catch (AmazonServiceException ase) {
            if (StringUtils.equals(ase.getErrorCode(),"InvalidKeyPair.NotFound")) {
                return false;
            }

            throw ase;
        }
    }

    /**
     * Determines all availability zones for a region that are marked as available in the primary region.
     *
     * @return List of availability zones
     */
    public List<String> getAvailabilityZones() {
        return getAvailabilityZones(configStore.getPrimaryRegion());
    }

    /**
     * Determines all availability zones for a region that are marked as available, in the provided region.
     *
     * @param region The region to use.
     * @return List of availability zones
     */
    public List<String> getAvailabilityZones(Regions region) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);
        DescribeAvailabilityZonesResult result = ec2Client.describeAvailabilityZones();

        return result.getAvailabilityZones()
                .stream()
                .filter(az -> AvailabilityZoneState.Available == AvailabilityZoneState.fromValue(az.getState()))
                .map(AvailabilityZone::getZoneName).collect(Collectors.toList());
    }

    /**
     * Gets all EC2 instances with the given tag key/value pair in the primary region
     *
     * @param tagKey   - Key of the tag
     * @param tagValue - Value of the tag
     * @param filters  - Array of EC2 filters
     * @return - List of instances with the given tag
     */
    public List<Instance> getInstancesByTag(String tagKey, String tagValue, Filter... filters) {
        return getInstancesByTag(configStore.getPrimaryRegion(), tagKey, tagValue, filters);
    }

    /**
     * Gets all EC2 instances with the given tag key/value pair
     *
     * @param region The region to use.
     * @param tagKey   - Key of the tag
     * @param tagValue - Value of the tag
     * @param filters  - Array of EC2 filters
     * @return - List of instances with the given tag
     */
    public List<Instance> getInstancesByTag(Regions region, String tagKey, String tagValue, Filter... filters) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);
        String filterName = String.format(FILTER_NAME_TEMPL_FOR_EC2_TAGS, tagKey);
        Filter tagFilter = new Filter().withName(filterName).withValues(tagValue);

        Set<Filter> filterSet = Sets.newHashSet(filters);
        filterSet.add(tagFilter);
        DescribeInstancesRequest request = new DescribeInstancesRequest().withFilters(filterSet);

        DescribeInstancesResult result = ec2Client.describeInstances(request);
        List<Instance> instances = Lists.newArrayList();

        result.getReservations().forEach(reservation -> {
            instances.addAll(reservation.getInstances());
        });

        return instances;
    }

    /**
     * Reboots the EC2 instance with the given ID
     *
     * @param instanceId - EC2 instance ID
     */
    public void rebootEc2Instance(String instanceId) {
        rebootEc2Instance(configStore.getPrimaryRegion(), instanceId);
    }

    /**
     * Reboots the EC2 instance with the given ID
     *
     * @param region The region to use.
     * @param instanceId - EC2 instance ID
     */
    public void rebootEc2Instance(Regions region, String instanceId) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);
        RebootInstancesRequest request = new RebootInstancesRequest().withInstanceIds(instanceId);
        ec2Client.rebootInstances(request);
    }

}
