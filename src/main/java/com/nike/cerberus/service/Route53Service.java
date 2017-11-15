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

import com.amazonaws.services.route53.AmazonRoute53;
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

/**
 * Service wrapper for AWS CloudFormation.
 */
public class Route53Service {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonRoute53 route53Client;

    @Inject
    public Route53Service(final AmazonRoute53 route53Client) {
        this.route53Client = route53Client;
    }

    public void createRoute53RecordSet(String hostedZoneId,
                                       String recordSetName,
                                       String recordValue,
                                       RRType recordSetType,
                                       String resourceRecordTtl) {
        logger.info("Creating Route53 record name: {}, value: {}", recordSetName, recordValue);

        final ResourceRecord record = new ResourceRecord().withValue(recordValue);
        final ResourceRecordSet recordSet = new ResourceRecordSet()
                .withResourceRecords(record)
                .withName(recordSetName)
                .withType(recordSetType)
                .withTTL(Long.parseLong(resourceRecordTtl));

        final ChangeBatch recordSetChangeBatch = new ChangeBatch()
                .withChanges(new Change()
                        .withAction(ChangeAction.CREATE)
                        .withResourceRecordSet(recordSet));

        route53Client.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
                .withHostedZoneId(hostedZoneId)
                .withChangeBatch(recordSetChangeBatch));
    }

    public boolean recordSetWithNameAlreadyExists(final String recordSetName, final String hostedZoneId) {
        final ListResourceRecordSetsResult recordSets = route53Client.listResourceRecordSets(
                new ListResourceRecordSetsRequest()
                        .withHostedZoneId(hostedZoneId));

        return recordSets.getResourceRecordSets()
                .stream()
                .anyMatch(recordSet -> recordSet.getName().equals(recordSetName + "."));
    }
}
