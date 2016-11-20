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

package com.nike.cerberus.operation.core;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2Service;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.RandomStringGenerator;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.nike.cerberus.ConfigConstants.MINIMUM_AZS;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Creates the base components via CloudFormation used by all of Cerberus.
 */
public class CreateBaseOperation implements Operation<CreateBaseCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2Service ec2Service;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    private final RandomStringGenerator passwordGenerator = new RandomStringGenerator();

    @Inject
    public CreateBaseOperation(final EnvironmentMetadata environmentMetadata,
                               final CloudFormationService cloudFormationService,
                               final Ec2Service ec2Service,
                               final UuidSupplier uuidSupplier,
                               final ConfigStore configStore,
                               @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2Service = ec2Service;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateBaseCommand command) {
        final String uniqueStackName = String.format("%s-%s", ConfigConstants.BASE_STACK_NAME, uuidSupplier.get());
        final Map<Integer, String> azByIdentifier = mapAvailabilityZones();
        final String dbMasterPassword = passwordGenerator.get();

        final BaseParameters baseParameters = new BaseParameters();
        baseParameters.setAccountAdminArn(command.getAdminRoleArn())
                .setAz1(azByIdentifier.get(1))
                .setAz2(azByIdentifier.get(2))
                .setAz3(azByIdentifier.get(3))
                .setCmsDbMasterUsername(ConfigConstants.DEFAULT_CMS_DB_NAME)
                .setCmsDbMasterPassword(dbMasterPassword)
                .setCmsDbName(ConfigConstants.DEFAULT_CMS_DB_NAME)
                .setVpcHostedZoneName(command.getVpcHostedZoneName());

        baseParameters.getTagParameters().setTagEmail(command.getOwnerEmail());
        baseParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        baseParameters.getTagParameters().setTagCostcenter(command.getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(baseParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.BASE_STACK_TEMPLATE_PATH, true);

        final StackStatus endStatus =
                cloudFormationService.waitForStatus(stackId,
                        Sets.newHashSet(StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE));

        if (StackStatus.CREATE_COMPLETE == endStatus) {
            final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);
            final BaseOutputs outputParameters =
                    cloudformationObjectMapper.convertValue(stackOutputs, BaseOutputs.class);

            logger.info("Stack creation complete, uploading data to the configuration bucket.");

            // ORDER IS IMPORTANT!

            // 1. First update the environment metadata with the new config bucket name.
            environmentMetadata.setBucketName(outputParameters.getConfigBucketName());

            // 2. Initialize the environment config.
            configStore.initEnvironmentData();

            // 3. Write the first configuration values to the config bucket.
            configStore.storeAzs(azByIdentifier.get(1), azByIdentifier.get(2), azByIdentifier.get(3));
            configStore.storeStackId(StackName.BASE, stackId);
            configStore.storeConfigKeyId(outputParameters.getConfigFileKeyId());

            // 4. Initialize the secrets config.
            configStore.initSecretsData();

            // 5. Write the first secret value to the config bucket once the config key id has been saved.
            configStore.storeCmsDatabasePassword(dbMasterPassword);

            logger.info("Uploading complete.");
        } else {
            final String errorMessage = String.format("Unexpected end status: %s", endStatus.name());
            logger.error(errorMessage);

            throw new UnexpectedCloudFormationStatusException(errorMessage);
        }
    }

    @Override
    public boolean isRunnable(final CreateBaseCommand command) {
        if (StringUtils.isNotBlank(environmentMetadata.getBucketName())) {
            try {
                final String stackId = configStore.getStackId(StackName.BASE);

                if (cloudFormationService.isStackPresent(stackId)) {
                    logger.warn("Operation has already run for the specified environment.");
                    return false;
                }
            } catch (IllegalStateException ise) { //NOPMD
                // Don't care, fall through.
            }
        }

        return true;
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
