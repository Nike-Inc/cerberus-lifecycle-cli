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

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.util.Map;
import java.util.UUID;

/**
 * Generic AWS Client factory that will create instances of AmazonWebServiceClients configured with the
 * proper credentials provider chain and configuration
 *
 * @param <T> The type of client that will be created.
 */
abstract public class AwsClientFactory<T extends AmazonWebServiceClient> {

    public static final String CERBERUS_ASSUME_ROLE_ARN = "CERBERUS_ASSUME_ROLE_ARN";
    public static final String CERBERUS_ASSUME_ROLE_EXTERNAL_ID = "CERBERUS_ASSUME_ROLE_EXTERNAL_ID";

    /**
     * Cache of clients by region
     */
    protected Map<Regions, T> clients = Maps.newHashMap();

    /**
     * Factory that creates and caches Aws clients by region for re-use;
     */
    public T getClient(Regions region) {
        if (!clients.containsKey(region)) {
            clients.put(region, createAmazonClientInstance(getGenericTypeClass(), region));
        }
        return clients.get(region);
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getGenericTypeClass() {
        TypeToken<T> typeToken = new TypeToken<T>(getClass()) {};
        return (Class<T>) typeToken.getRawType();
    }

    protected <M extends AmazonWebServiceClient> M createAmazonClientInstance(Class<M> clientClass, Regions region) {
        return Region.getRegion(region)
                .createClient(clientClass, getAWSCredentialsProviderChain(), getClientConfiguration());
    }

    protected ClientConfiguration getClientConfiguration() {
        return new ClientConfiguration();
    }

    protected AWSCredentialsProviderChain getAWSCredentialsProviderChain() {
        String cerberusRoleToAssume = System.getenv(CERBERUS_ASSUME_ROLE_ARN) != null ?
                System.getenv(CERBERUS_ASSUME_ROLE_ARN) : "";
        String cerberusRoleToAssumeExternalId = System.getenv(CERBERUS_ASSUME_ROLE_EXTERNAL_ID) != null ?
                System.getenv(CERBERUS_ASSUME_ROLE_EXTERNAL_ID) : "";

        STSAssumeRoleSessionCredentialsProvider sTSAssumeRoleSessionCredentialsProvider =
                new STSAssumeRoleSessionCredentialsProvider
                        .Builder(cerberusRoleToAssume, UUID.randomUUID().toString())
                        .withExternalId(cerberusRoleToAssumeExternalId)
                        .build();

        AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new ProfileCredentialsProvider(),
                sTSAssumeRoleSessionCredentialsProvider,
                InstanceProfileCredentialsProvider.getInstance(),
                new EC2ContainerCredentialsProviderWrapper());

        return chain;
    }

}
