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

import com.beust.jcommander.internal.Maps;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.nike.cerberus.ConfigConstants.MINIMUM_AZS;
import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateVpcOperation implements Operation<CreateVpcCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final Ec2Service ec2Service;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final ConfigStore configStore;

    @Inject
    public CreateVpcOperation(@Named(ENV_NAME) String environmentName,
                              CloudFormationService cloudFormationService,
                              Ec2Service ec2Service,
                              CloudFormationObjectMapper cloudFormationObjectMapper,
                              ConfigStore configStore) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.ec2Service = ec2Service;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.configStore = configStore;
    }

    @Override
    public void run(CreateVpcCommand command) {
        Map<Integer, String> azByIdentifier = mapAvailabilityZones();

        VpcParameters vpcParameters = new VpcParameters()
                .setAz1(azByIdentifier.get(1))
                .setAz2(azByIdentifier.get(2))
                .setAz3(azByIdentifier.get(3))
                .setEnvironmentName(environmentName);

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(vpcParameters);

        cloudFormationService.createStackAndWait(
                configStore.getPrimaryRegion(),
                Stack.VPC,
                parameters,
                true,
                command.getTagsDelegate().getTags());
    }

    @Override
    public boolean isRunnable(CreateVpcCommand command) {
        return !cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.VPC.getFullName(environmentName));
    }

    private Map<Integer, String> mapAvailabilityZones() {
        List<String> zones = ec2Service.getAvailabilityZones();

        if (zones.size() < MINIMUM_AZS) {
            throw new IllegalStateException("Not enough availability zones for the selected region.");
        }

        Map<Integer, String> azByIdentifier = Maps.newHashMap();

        for (int i = 1; i <= MINIMUM_AZS; i++) {
            azByIdentifier.put(i, zones.get(i - 1));
        }

        return azByIdentifier;
    }
}
