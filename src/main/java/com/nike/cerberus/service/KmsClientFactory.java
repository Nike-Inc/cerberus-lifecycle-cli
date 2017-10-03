/*
 * Copyright (c) 2017 Nike, Inc.
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
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.beust.jcommander.internal.Maps;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Factory that caches kmsClients for re-use.
 */
@Singleton
public class KmsClientFactory {

    /**
     * Cache of kmsClients by region
     */
    private Map<Regions, AWSKMS> kmsClients = Maps.newHashMap();

    public AWSKMS getClient(String regionName) {
        return getClient(Regions.fromName(regionName));
    }

    /**
     * Factory that caches kmsClients for re-use.
     */
    public AWSKMS getClient(Regions region) {
        if (!kmsClients.containsKey(region)) {
            kmsClients.put(region, AWSKMSClientBuilder.standard().withRegion(region).build());
        }
        return kmsClients.get(region);
    }
}
