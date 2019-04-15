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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Service wrapper for AWS CloudFormation.
 */
public class Route53Service {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AwsClientFactory<AmazonRoute53Client> route53ClientFactory;

    @Inject
    public Route53Service(AwsClientFactory<AmazonRoute53Client> route53ClientFactory) {

        // not region specific config region works
        this.route53ClientFactory = route53ClientFactory;
    }

    public void createRoute53RecordSet(String hostedZoneId,
                                       String recordSetName,
                                       String recordValue,
                                       RRType recordSetType,
                                       String resourceRecordTtl,
                                       Regions region) {

        logger.info("Creating Route53 record name: {}, value: {}", recordSetName, recordValue);

        ResourceRecord record = new ResourceRecord().withValue(recordValue);
        ResourceRecordSet recordSet = new ResourceRecordSet()
                .withResourceRecords(record)
                .withName(recordSetName)
                .withType(recordSetType)
                .withTTL(Long.parseLong(resourceRecordTtl));

        ChangeBatch recordSetChangeBatch = new ChangeBatch()
                .withChanges(new Change()
                        .withAction(ChangeAction.UPSERT)
                        .withResourceRecordSet(recordSet));

        route53ClientFactory.getClient(region).changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
                .withHostedZoneId(hostedZoneId)
                .withChangeBatch(recordSetChangeBatch));
    }

    public Optional<ResourceRecordSet> getRecordSetByName(String recordSetName,
                                                          String hostedZoneId,
                                                          Regions region) {

        ListResourceRecordSetsResult recordSets = route53ClientFactory.getClient(region).listResourceRecordSets(
                new ListResourceRecordSetsRequest()
                        .withHostedZoneId(hostedZoneId));

        for (ResourceRecordSet recordSet : recordSets.getResourceRecordSets()) {
            if (recordSet.getName().equals(recordSetName + ".")) {
                return Optional.of(recordSet);
            }
        }

        return Optional.empty();
    }
}
