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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.store.ConfigStore;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Service wrapper for AWS EC2.
 */
public class AmiTagCheckService {

    private final AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;
    private final ConfigStore configStore;

    private final Map<Stack, String> stackAmiTagValueMap;

    @Inject
    public AmiTagCheckService(AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory,
                              ConfigStore configStore) {

        this.amazonEC2ClientFactory = amazonEC2ClientFactory;
        this.configStore = configStore;

        stackAmiTagValueMap = new HashMap<>();
        stackAmiTagValueMap.put(Stack.CMS, ConfigConstants.CMS_AMI_TAG_VALUE);
    }

    /**
     * Validates if the given AMI has given tag and value in the primary region.
     *
     * @return true if matches otherwise false
     */
    public boolean isAmiWithTagExist(String amiId, String tagName, String tagValue) {
        return isAmiWithTagExist(configStore.getPrimaryRegion(), amiId, tagName, tagValue);
    }

    /**
     * Validates if the given AMI has given tag and value in the provided region.
     *
     * @return true if matches otherwise false
     */
    public boolean isAmiWithTagExist(Regions region, String amiId, String tagName, String tagValue) {
        AmazonEC2 ec2Client = amazonEC2ClientFactory.getClient(region);

        DescribeImagesRequest request = new DescribeImagesRequest()
                .withFilters(new Filter().withName(tagName).withValues(tagValue))
                .withFilters(new Filter().withName("image-id").withValues(amiId));

        try {
            DescribeImagesResult result = ec2Client.describeImages(request);
            return result.getImages().size() > 0;
        } catch (final AmazonServiceException ase) {
            if (ase.getErrorCode() == "InvalidAMIID.NotFound") {
                return false;
            }

            throw ase;
        }
    }

    /**
     * Validates if the given AMI has given cerberus tag and value.
     *
     * @return void
     */
    public void validateAmiTagForStack(final String amiId, final Stack stack) {
        if (!isAmiWithTagExist(amiId, ConfigConstants.CERBERUS_AMI_TAG_NAME, stackAmiTagValueMap.get(stack))) {
            throw new IllegalStateException(ConfigConstants.AMI_TAG_CHECK_ERROR_MESSAGE);
        }
    }
}
