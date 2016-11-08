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

package com.nike.cerberus.operation.gateway;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.gateway.CreateGatewayClusterCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.GatewayParameters;
import com.nike.cerberus.domain.environment.LambdaName;
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
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation for creating the gateway cluster.
 */
public class CreateGatewayClusterOperation implements Operation<CreateGatewayClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateGatewayClusterOperation(final EnvironmentMetadata environmentMetadata,
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
    public void run(final CreateGatewayClusterCommand command) {
        final String uniqueStackName = String.format("%s-%s", StackName.GATEWAY.getName(), uuidSupplier.get());
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final Optional<String> gatewayServerCertificateArn = configStore.getServerCertificateArn(StackName.GATEWAY);
        final Optional<String> gatewayServerCertificateId = configStore.getServerCertificateId(StackName.GATEWAY);
        final Optional<String> pubKey = configStore.getCertPart(StackName.GATEWAY, ConfigConstants.CERT_PART_PUBKEY);

        if (!gatewayServerCertificateArn.isPresent() || !pubKey.isPresent()) {
            throw new IllegalStateException("Gateway certificate has not been uploaded!");
        }

        final GatewayParameters gatewayParameters = new GatewayParameters()
                .setVpcId(baseOutputs.getVpcId())
                .setInstanceProfileName(baseOutputs.getGatewayInstanceProfileName())
                .setGatewayServerSgId(baseOutputs.getGatewayServerSgId())
                .setGatewayElbSgId(baseOutputs.getGatewayElbSgId())
                .setToolsIngressSgId(baseOutputs.getToolsIngressSgId())
                .setVpcSubnetIdForAz1(baseOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(baseOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(baseOutputs.getVpcSubnetIdForAz3())
                .setHostedZoneId(command.getHostedZoneId())
                .setHostname(command.getHostname())
                .setWafLambdaBucket(environmentMetadata.getBucketName())
                .setWafLambdaKey(LambdaName.WAF.getBucketKey())
                .setCloudFrontLogProcessorLambdaIamRoleArn(baseOutputs.getCloudFrontLogProcessorLambdaIamRoleArn());

        gatewayParameters.getSslConfigParameters().setCertPublicKey(pubKey.get());
        gatewayParameters.getSslConfigParameters().setSslCertificateArn(gatewayServerCertificateArn.get());
        gatewayParameters.getSslConfigParameters().setSslCertificateId(gatewayServerCertificateId.get());

        gatewayParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        gatewayParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        gatewayParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        gatewayParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.GATEWAY, command.getStackDelegate().getOwnerGroup()));

        gatewayParameters.getTagParameters().setTagEmail(command.getStackDelegate().getOwnerEmail());
        gatewayParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        gatewayParameters.getTagParameters().setTagCostcenter(command.getStackDelegate().getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(gatewayParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.GATEWAY_STACK_TEMPLATE_PATH, true);

        configStore.storeStackId(StackName.GATEWAY, stackId);

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
    public boolean isRunnable(final CreateGatewayClusterCommand command) {
        boolean isRunnable = true;
        final String cmsStackId = configStore.getStackId(StackName.CMS);
        final String certificateName = configStore.getServerCertificateName(StackName.GATEWAY);
        final boolean hasGatewayConfig = configStore.hasGatewayConfig();

        if (StringUtils.isBlank(cmsStackId) || !cloudFormationService.isStackPresent(cmsStackId)) {
            logger.error("No CMS stack defined for this environment!");
            isRunnable = false;
        }

        if (StringUtils.isBlank(certificateName)) {
            logger.error("Certificate has not been uploaded for Gateway!");
            isRunnable = false;
        }

        if (!hasGatewayConfig) {
            logger.error("No configuration for Gateway exists for this environment!");
            isRunnable = false;
        }

        return isRunnable;
    }
}
