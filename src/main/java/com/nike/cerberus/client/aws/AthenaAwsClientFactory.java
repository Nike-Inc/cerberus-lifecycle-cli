package com.nike.cerberus.client.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.athena.AmazonAthenaClient;
import com.nike.cerberus.service.AwsClientFactory;

public class AthenaAwsClientFactory extends AwsClientFactory<AmazonAthenaClient> {

    @Override
    public AmazonAthenaClient getClient(Regions region) {
        if (!clients.containsKey(region)) {
            clients.put(region, createClient(region));
        }
        return clients.get(region);
    }

    private AmazonAthenaClient createClient(Regions region) {
        return (AmazonAthenaClient) AmazonAthenaClient.builder()
                .withRegion(region)
                .withCredentials(getAWSCredentialsProviderChain())
                .build();
    }

}
