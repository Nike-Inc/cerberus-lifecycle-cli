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

package com.nike.cerberus.operation.cms;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.service.AmiTagCheckService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Operation for creating the CMS cluster.
 */
public class CreateCmsClusterOperation implements Operation<CreateCmsClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final AmiTagCheckService amiTagCheckService;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final ObjectMapper cloudformationObjectMapper;

    @Inject
    public CreateCmsClusterOperation(final EnvironmentMetadata environmentMetadata,
                                     final CloudFormationService cloudFormationService,
                                     final Ec2UserDataService ec2UserDataService,
                                     final AmiTagCheckService amiTagCheckService,
                                     final UuidSupplier uuidSupplier,
                                     final ConfigStore configStore,
                                     @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudformationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.amiTagCheckService = amiTagCheckService;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.cloudformationObjectMapper = cloudformationObjectMapper;
    }

    @Override
    public void run(final CreateCmsClusterCommand command) {
        final String uniqueStackName = String.format("%s-%s", StackName.CMS.getName(), uuidSupplier.get());
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final Optional<String> cmsServerCertificateArn = configStore.getServerCertificateArn(StackName.CMS);
        final Optional<String> pubKey = configStore.getCertPart(StackName.CMS, ConfigConstants.CERT_PART_PUBKEY);
        final String internalElbCname = configStore.getInternalElbCname(StackName.CMS);

        if (!cmsServerCertificateArn.isPresent() || !pubKey.isPresent()) {
            throw new IllegalStateException("CMS certificate has not been uploaded!");
        }

        // Make sure the given AmiId is for CMS component. Check if it contains required tag
        if ( !command.isSkipAmiTagCheck() ) {
            amiTagCheckService.validateAmiTagForStack(command.getStackDelegate().getAmiId(), StackName.CMS);
        }

        final CmsParameters cmsParameters = new CmsParameters()
                .setInstanceProfileName(baseOutputs.getCmsInstanceProfileName())
                .setCmsElbSgId(baseOutputs.getCmsElbSgId())
                .setCmsSgId(baseOutputs.getCmsSgId())
                .setToolsIngressSgId(baseOutputs.getToolsIngressSgId())
                .setVpcId(baseOutputs.getVpcId())
                .setVpcSubnetIdForAz1(baseOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(baseOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(baseOutputs.getVpcSubnetIdForAz3())
                .setHostedZoneId(baseOutputs.getVpcHostedZoneId())
                .setCname(internalElbCname);

        cmsParameters.getSslConfigParameters().setCertPublicKey(pubKey.get());
        cmsParameters.getSslConfigParameters().setSslCertificateArn(cmsServerCertificateArn.get());

        cmsParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        cmsParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        cmsParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        cmsParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.CMS, command.getStackDelegate().getOwnerGroup()));
        cmsParameters.getLaunchConfigParameters().setDesiredInstances(command.getStackDelegate().getDesiredInstances());
        cmsParameters.getLaunchConfigParameters().setMinimumInstances(command.getStackDelegate().getMinimumInstances());
        cmsParameters.getLaunchConfigParameters().setMaximumInstances(command.getStackDelegate().getMaximumInstances());

        cmsParameters.getTagParameters().setTagEmail(command.getStackDelegate().getOwnerEmail());
        cmsParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        cmsParameters.getTagParameters().setTagCostcenter(command.getStackDelegate().getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudformationObjectMapper.convertValue(cmsParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(uniqueStackName),
                parameters, ConfigConstants.CMS_STACK_TEMPLATE_PATH, true);

        logger.info("Uploading data to the configuration bucket.");
        configStore.storeStackId(StackName.CMS, stackId);
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
    public boolean isRunnable(final CreateCmsClusterCommand command) {
        return configStore.getCmsEnvConfig().isPresent();
    }
}
