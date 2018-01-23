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
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.service.Route53Service;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the edge domain Route53 record for Cerberus
 */
public class CreateEdgeDomainRecordOperation implements Operation<CreateEdgeDomainRecordCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String RESOURCE_RECORD_TTL = "30";  // in seconds

    private final CloudFormationService cloudFormationService;

    private final ConfigStore configStore;

    private final String environmentName;

    private final Route53Service route53Service;

    private final ConsoleService consoleService;

    @Inject
    public CreateEdgeDomainRecordOperation(CloudFormationService cloudFormationService,
                                           ConfigStore configStore,
                                           @Named(ENV_NAME) String environmentName,
                                           Route53Service route53Service,
                                           ConsoleService consoleService) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.environmentName = environmentName;
        this.route53Service = route53Service;
        this.consoleService = consoleService;
    }

    @Override
    public void run(CreateEdgeDomainRecordCommand command) {
        String recordValue = configStore.getRoute53StackOutputs().getOriginDomainName();
        String recordSetName = getEdgeDomainName(command.getBaseDomainName(), command.getEdgeDomainNameOverride());

        route53Service.createRoute53RecordSet(command.getHostedZoneId(),
                recordSetName,
                recordValue,
                RRType.CNAME,
                RESOURCE_RECORD_TTL);
    }

    @Override
    public boolean isRunnable(CreateEdgeDomainRecordCommand command) {
        String recordSetName = getEdgeDomainName(command.getBaseDomainName(), command.getEdgeDomainNameOverride());

        boolean isRunnable = true;

        if (!cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.ROUTE53.getFullName(environmentName))) {
            logger.error("The route53 stack must be present");
            return false;
        }

        Optional<ResourceRecordSet> edgeRecordOptional = route53Service
                .getRecordSetByName(recordSetName, command.getHostedZoneId());

        if (edgeRecordOptional.isPresent()) {
            String msg = String.format(
                    "The edge domain: '%s' already has a record set in hosted zone: '%s'  with type: '%s' and value: " +
                            "'%s', if you proceed this will be overridden to value = '%s'",
                    recordSetName,
                    command.getHostedZoneId(), edgeRecordOptional.get().getType(),
                    edgeRecordOptional.get().getResourceRecords()
                            .stream().map(ResourceRecord::getValue)
                            .collect(Collectors.joining(", ")),
                    configStore.getRoute53StackOutputs().getOriginDomainName()
            );

            try {
                consoleService.askUserToProceed(msg, ConsoleService.DefaultAction.NO);
            } catch (RuntimeException e) {
                isRunnable = false;
            }
        }
        return isRunnable;
    }

    private String getEdgeDomainName(String baseDomainName, String edgeDomainNameOverride) {
        String defaultEdgeDomainName = String.format("%s.%s",
                environmentName,
                baseDomainName);

        return StringUtils.isBlank(edgeDomainNameOverride) ?
                defaultEdgeDomainName : edgeDomainNameOverride;
    }
}
