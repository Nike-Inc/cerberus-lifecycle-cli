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

package com.nike.cerberus.operation.core;

import com.amazonaws.services.route53.model.RRType;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Route53Service;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Creates the edge domain Route53 record for Cerberus
 */
public class CreateEdgeDomainRecordOperation implements Operation<CreateEdgeDomainRecordCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String RESOURCE_RECORD_TTL = "30";  // in seconds

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final EnvironmentMetadata environmentMetadata;

    private final Route53Service route53Service;

    @Inject
    public CreateEdgeDomainRecordOperation(final CloudFormationService cloudFormationService,
                                           final ConfigStore configStore,
                                           final EnvironmentMetadata environmentMetadata,
                                           final Route53Service route53Service) {
        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.environmentMetadata = environmentMetadata;
        this.route53Service = route53Service;
    }

    @Override
    public void run(final CreateEdgeDomainRecordCommand command) {
        final String recordValue = configStore.getRoute53StackOutputs().getOriginDomainName();
        final String recordSetName = getEdgeDomainName(command.getBaseDomainName(), command.getEdgeDomainNameOverride());

        route53Service.createRoute53RecordSet(command.getHostedZoneId(),
                recordSetName,
                recordValue,
                RRType.CNAME,
                RESOURCE_RECORD_TTL);
    }

    @Override
    public boolean isRunnable(final CreateEdgeDomainRecordCommand command) {
        final String environmentName = environmentMetadata.getName();
        final String recordSetName = getEdgeDomainName(command.getBaseDomainName(), command.getEdgeDomainNameOverride());

        try {
            cloudFormationService.getStackId(Stack.ROUTE53.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("The load balancer stack must exist to create the Route53 record!", iae);
        }

        return !route53Service.recordSetWithNameAlreadyExists(recordSetName, command.getHostedZoneId());
    }

    private String getEdgeDomainName(String baseDomainName, final String edgeDomainNameOverride) {
        final String defaultEdgeDomainName = String.format("%s.%s",
                environmentMetadata.getName(),
                baseDomainName);

        return StringUtils.isBlank(edgeDomainNameOverride) ?
                defaultEdgeDomainName : edgeDomainNameOverride;
    }
}
