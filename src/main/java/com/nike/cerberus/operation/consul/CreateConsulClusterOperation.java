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

package com.nike.cerberus.operation.consul;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.consul.CreateConsulClusterCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.ConsulParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation to create the Consul cluster.
 */
public class CreateConsulClusterOperation implements Operation<CreateConsulClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateConsulClusterOperation(final EnvironmentMetadata environmentMetadata,
                               final CloudFormationService cloudFormationService,
                               final Ec2UserDataService ec2UserDataService,
                               final UuidSupplier uuidSupplier,
                               final ConfigStore configStore,
                               @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateConsulClusterCommand command) {
        final String uniqueStackName = String.format("%s-%s", StackName.CONSUL.getName(), uuidSupplier.get());
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();

        final ConsulParameters consulParameters = new ConsulParameters()
                .setInstanceProfileName(baseOutputs.getConsulInstanceProfileName())
                .setConsulClientSgId(baseOutputs.getConsulClientSgId())
                .setConsulServerSgId(baseOutputs.getConsulServerSgId())
                .setToolsIngressSgId(baseOutputs.getToolsIngressSgId())
                .setVpcId(baseOutputs.getVpcId())
                .setVpcSubnetIdForAz1(baseOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(baseOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(baseOutputs.getVpcSubnetIdForAz3());

        consulParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        consulParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        consulParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        consulParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        consulParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.CONSUL, command.getStackDelegate().getOwnerGroup()));

        consulParameters.getTagParameters().setTagEmail(command.getStackDelegate().getOwnerEmail());
        consulParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        consulParameters.getTagParameters().setTagCostcenter(command.getStackDelegate().getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(consulParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.CONSUL_STACK_TEMPLATE_PATH, true);

        logger.info("Uploading data to the configuration bucket.");
        configStore.storeStackId(StackName.CONSUL, stackId);
        logger.info("Uploading complete.");

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (endStatus != StackStatus.CREATE_COMPLETE) {
            final String errorMessage = String.format("Unexpected end status: %s", endStatus.name());
            logger.error(errorMessage);

            throw new UnexpectedCloudFormationStatusException(errorMessage);
        }
    }

    @Override
    public boolean isRunnable(final CreateConsulClusterCommand command) {
        boolean isRunnable = true;
        final String baseStackId = configStore.getStackId(StackName.BASE);
        final boolean hasConsulConfig = configStore.hasConsulConfig();
        final boolean hasVaultAcl = configStore.hasVaultAclEntry();

        if (StringUtils.isBlank(baseStackId) || !cloudFormationService.isStackPresent(baseStackId)) {
            logger.error("No base stack defined for this environment!");
            isRunnable = false;
        }

        if (!hasConsulConfig) {
            logger.error("No configuration for Consul exists for this environment!");
            isRunnable = false;
        }

        if (!hasVaultAcl) {
            logger.error("Vault ACL has not been generated for this environment!");
            isRunnable = false;
        }

        return isRunnable;
    }
}
