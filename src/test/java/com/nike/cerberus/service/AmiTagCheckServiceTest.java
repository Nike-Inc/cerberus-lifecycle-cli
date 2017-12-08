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
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.nike.cerberus.store.ConfigStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AmiTagCheckServiceTest {

    @Mock
    private ConfigStore configStore;

    @Mock
    private AmazonEC2Client ec2Client;

    @Mock
    private AwsClientFactory<AmazonEC2Client> amazonEC2ClientFactory;

    @Before
    public void before() {
        initMocks(this);
        when(amazonEC2ClientFactory.getClient(any())).thenReturn(ec2Client);
    }

    @Test
    public void isAmiWithTagExistTrue() {
        AmiTagCheckService amiTagCheckService = new AmiTagCheckService(amazonEC2ClientFactory, configStore);

        String amiId = "ami-1234abcd";
        String tagName = "sometag";
        String tagValue = "someval";

        when(ec2Client.describeImages(
                new DescribeImagesRequest()
                        .withFilters(new Filter().withName(tagName).withValues(tagValue))
                        .withFilters(new Filter().withName("image-id").withValues(amiId))
                )
        ).thenReturn(
                new DescribeImagesResult().withImages(new Image())
        );

        // invoke method under test
        assertTrue(amiTagCheckService.isAmiWithTagExist(amiId, tagName, tagValue));
    }

    @Test
    public void isAmiWithTagExistFalse() {
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        AmiTagCheckService amiTagCheckService = new AmiTagCheckService(amazonEC2ClientFactory, configStore);

        String amiId = "ami-1234abcd";
        String tagName = "sometag";
        String tagValue = "someval";

        when(ec2Client.describeImages(
                new DescribeImagesRequest()
                        .withFilters(new Filter().withName(tagName).withValues(tagValue))
                        .withFilters(new Filter().withName("image-id").withValues(amiId))
                )
        ).thenReturn(
                new DescribeImagesResult()
        );

        // invoke method under test
        assertFalse(amiTagCheckService.isAmiWithTagExist(amiId, tagName, tagValue));
    }

    @Test
    public void isAmiWithTagExistNotFound() {
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        AmiTagCheckService amiTagCheckService = new AmiTagCheckService(amazonEC2ClientFactory, configStore);

        String amiId = "ami-1234abcd";
        String tagName = "sometag";
        String tagValue = "someval";

        AmazonServiceException ex = new AmazonServiceException("fake-exception");
        ex.setErrorCode("InvalidAMIID.NotFound");

        when(ec2Client.describeImages(
                new DescribeImagesRequest()
                        .withFilters(new Filter().withName(tagName).withValues(tagValue))
                        .withFilters(new Filter().withName("image-id").withValues(amiId))
                )
        ).thenThrow(ex);

        // invoke method under test
        assertFalse(amiTagCheckService.isAmiWithTagExist(amiId, tagName, tagValue));
    }

    @Test
    public void isAmiWithTagExistThrowException() {
        AmazonEC2 ec2Client = mock(AmazonEC2.class);
        AmiTagCheckService amiTagCheckService = new AmiTagCheckService(amazonEC2ClientFactory, configStore);

        String amiId = "ami-1234abcd";
        String tagName = "sometag";
        String tagValue = "someval";
        String unknownAwsExMessage = "Unknown AWS exception message";

        when(ec2Client.describeImages(
                new DescribeImagesRequest()
                        .withFilters(new Filter().withName(tagName).withValues(tagValue))
                        .withFilters(new Filter().withName("image-id").withValues(amiId))
                )
        ).thenThrow(new AmazonServiceException(unknownAwsExMessage));

        try {
            // invoke method under test
            amiTagCheckService.isAmiWithTagExist(amiId, tagName, tagValue);
            fail("Expected exception message '" + unknownAwsExMessage + "'not received");
        } catch (AmazonServiceException ex) {
            // pass
            assertEquals(unknownAwsExMessage, ex.getErrorMessage());
        }
    }
}