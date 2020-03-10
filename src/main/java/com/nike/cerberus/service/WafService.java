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
import com.amazonaws.services.waf.AWSWAFRegionalClient;
import com.amazonaws.services.waf.model.FieldToMatch;
import com.amazonaws.services.waf.model.LoggingConfiguration;
import com.amazonaws.services.waf.model.MatchFieldType;
import com.amazonaws.services.waf.model.PutLoggingConfigurationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Service wrapper for WAF.
 */
public class WafService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AwsClientFactory<AWSWAFRegionalClient> wafClientFactory;

    private final List<FieldToMatch> redactedFields;

    @Inject
    public WafService(AwsClientFactory<AWSWAFRegionalClient> wafClientFactory) {
        this.wafClientFactory = wafClientFactory;
        redactedFields = getredactedFields("authorization", "x-amz-security-token", "x-cerberus-token");
    }

    private List<FieldToMatch> getredactedFields(String... headerNames) {
        List<FieldToMatch> fieldToMatchList = new ArrayList<>();
        for (String headerName:headerNames){
            fieldToMatchList.add(new FieldToMatch().withType(MatchFieldType.HEADER).withData(headerName));
        }
        return fieldToMatchList;
    }

    public void enableWafLogging(String kinesisFirehoseArn, String webAclArn, Regions region) {
        AWSWAFRegionalClient wafClient = wafClientFactory.getClient(region);
        wafClient.putLoggingConfiguration(new PutLoggingConfigurationRequest().withLoggingConfiguration(new LoggingConfiguration()
                .withRedactedFields(redactedFields).withResourceArn(webAclArn).withLogDestinationConfigs(kinesisFirehoseArn)));
    }
}
