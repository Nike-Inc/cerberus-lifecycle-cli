/*
 * Copyright (c) 2020 Nike, Inc.
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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.model.StartDeliveryStreamEncryptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Service wrapper for Kinesis.
 */
public class KinesisService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AwsClientFactory<AmazonKinesisFirehoseClient> kinesisFirehoseClientFactory;

    @Inject
    public KinesisService(AwsClientFactory<AmazonKinesisFirehoseClient> kinesisFirehoseClientFactory) {
        this.kinesisFirehoseClientFactory = kinesisFirehoseClientFactory;
    }

    public void enableEncryption(String deliveryStreamName, Regions region) {
        AmazonKinesisFirehoseClient kinesisFirehoseClient = kinesisFirehoseClientFactory.getClient(region);
        kinesisFirehoseClient.startDeliveryStreamEncryption(new StartDeliveryStreamEncryptionRequest()
                .withDeliveryStreamName(deliveryStreamName));
    }
}
