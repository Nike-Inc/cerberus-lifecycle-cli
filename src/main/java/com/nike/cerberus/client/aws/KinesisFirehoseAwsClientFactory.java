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

package com.nike.cerberus.client.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.nike.cerberus.service.AwsClientFactory;

public class KinesisFirehoseAwsClientFactory extends AwsClientFactory<AmazonKinesisFirehoseClient> {

    @Override
    public AmazonKinesisFirehoseClient getClient(Regions region) {
        if (!clients.containsKey(region)) {
            clients.put(region, createClient(region));
        }
        return clients.get(region);
    }

    private AmazonKinesisFirehoseClient createClient(Regions region) {
        return (AmazonKinesisFirehoseClient) AmazonKinesisFirehoseClientBuilder.standard()
                .withRegion(region)
                .withCredentials(getAWSCredentialsProviderChain())
                .build();
    }

}
