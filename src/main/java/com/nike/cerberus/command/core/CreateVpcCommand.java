/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Map;

import static com.nike.cerberus.ConfigConstants.MINIMUM_AZS;
import static com.nike.cerberus.command.core.CreateVpcCommand.COMMAND_NAME;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Command for creating the base components for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the VPC in which Cerberus components live")
public class CreateVpcCommand implements Command {

    public static final String COMMAND_NAME = "create-vpc";

    public static final String STACK_NAME = "cerberus-vpc";

    public static final String OWNER_EMAIL_LONG_ARG = "--owner-email";

    public static final String COST_CENTER_LONG_ARG = "--costcenter";

    @Parameter(names = OWNER_EMAIL_LONG_ARG,
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.",
            required = true)
    private String ownerEmail;

    @Parameter(names = COST_CENTER_LONG_ARG,
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.",
            required = true)
    private String costcenter;

    private EnvironmentMetadata environmentMetadata;

    private CloudFormationService cloudFormationService;

    private Ec2Service ec2Service;

    private ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateVpcCommand(final EnvironmentMetadata environmentMetadata,
                            final CloudFormationService cloudFormationService,
                            final Ec2Service ec2Service,
                            @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudFormationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2Service = ec2Service;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public void execute() {
        final String environmentName = environmentMetadata.getName();
        final String stackName = String.format("%s-%s", environmentMetadata.getName(), STACK_NAME);
        final Map<Integer, String> azByIdentifier = mapAvailabilityZones();

        final VpcParameters vpcParameters = new VpcParameters();
        vpcParameters.setAz1(azByIdentifier.get(1))
                .setAz2(azByIdentifier.get(2))
                .setAz3(azByIdentifier.get(3));

        vpcParameters.getTagParameters().setTagEmail(ownerEmail);
        vpcParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentName);
        vpcParameters.getTagParameters().setTagCostcenter(costcenter);

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(vpcParameters, typeReference);

        cloudFormationService.createStack(cloudFormationService.getEnvStackName(stackName),
                parameters, ConfigConstants.VPC_STACK_TEMPLATE_PATH, true);
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

    @Override
    public boolean isRunnable() {
        return true;
    }
}
