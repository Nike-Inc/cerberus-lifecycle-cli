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
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

import javax.inject.Inject;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.ConfigConstants;

/**
 * Service wrapper for AWS EC2.
 */
public class AmiTagCheckService {

    private final AmazonEC2 ec2Client;

    private final Map<StackName, String> stackAmiTagValueMap;

    @Inject
    public AmiTagCheckService(final AmazonEC2 ec2Client) {
        this.ec2Client = ec2Client;

        stackAmiTagValueMap = new HashMap<>();
        stackAmiTagValueMap.put(StackName.CONSUL, ConfigConstants.CONSUL_AMI_TAG_VALUE);
        stackAmiTagValueMap.put(StackName.VAULT, ConfigConstants.VAULT_AMI_TAG_VALUE);
        stackAmiTagValueMap.put(StackName.CMS, ConfigConstants.CMS_AMI_TAG_VALUE);
        stackAmiTagValueMap.put(StackName.GATEWAY, ConfigConstants.GATEWAY_AMI_TAG_VALUE);

    }

    /**
     * Validates if the given AMI has given tag and value.
     *
     * @return true if matches otherwise false
     */
    public boolean isAmiWithTagExist(final String amiId, final String tagName, final String tagValue) {

        final DescribeImagesRequest request = new DescribeImagesRequest()
            .withFilters(new Filter().withName(tagName).withValues(tagValue))
            .withFilters(new Filter().withName("image-id").withValues(amiId));

        try {
            final DescribeImagesResult result = ec2Client.describeImages(request);
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
    public void validateAmiTagForStack(final String amiId, final StackName stackName) {
        if (!isAmiWithTagExist(amiId, ConfigConstants.CERBERUS_AMI_TAG_NAME, stackAmiTagValueMap.get(stackName) )) {
            throw new IllegalStateException("AMI tag check failed!. Given AMI ID does not contain cerberus tag 'cerberus_component' with stack name");
        }
    }
}
