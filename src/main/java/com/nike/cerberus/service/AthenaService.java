/*
 * Copyright (c) 2018 Nike, Inc.
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
import com.amazonaws.services.athena.AmazonAthenaClient;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Service wrapper for AWS CloudFormation.
 */
public class AthenaService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AwsClientFactory<AmazonAthenaClient> athenaClientFactory;

    @Inject
    public AthenaService(AwsClientFactory<AmazonAthenaClient> athenaClientFactory) {

        this.athenaClientFactory = athenaClientFactory;
    }

    /**
     * Executes an Athena query and waits for it to finish returning the results
     */
    public GetQueryResultsResult executeAthenaQuery(String query, String bucketName, Regions region) {
        AmazonAthenaClient athena = athenaClientFactory.getClient(region);
        StartQueryExecutionResult result = athena
                .startQueryExecution(new StartQueryExecutionRequest()
                        .withQueryString(query)
                        .withResultConfiguration(new ResultConfiguration().withOutputLocation(String.format("s3://%s/results/", bucketName)))
                );

        String id = result.getQueryExecutionId();

        String state;
        do {
            state = athena.getQueryExecution(new GetQueryExecutionRequest().withQueryExecutionId(id)).getQueryExecution().getStatus().getState();
            log.info("polling for query to finish: current status: {}", state);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Failed to sleep", e);
                Thread.currentThread().interrupt();
            }
        } while (state.equals("RUNNING"));

        log.info("The query: {} is in state: {}, fetching results", id, state);

        return athena.getQueryResults(new GetQueryResultsRequest().withQueryExecutionId(id));
    }
}
