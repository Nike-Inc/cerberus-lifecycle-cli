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

package com.nike.cerberus.command.cms;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.command.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.AmiTagCheckService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.command.cms.CreateCmsClusterCommand.COMMAND_NAME;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_ARG;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_DESCRIPTION;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the CMS cluster.")
public class CreateCmsClusterCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "create-cms-cluster";

    public static final String STACK_NAME = "cerberus-cms-cluster";

    @ParametersDelegate
    private StackDelegate stackDelegate = new StackDelegate();

    @Parameter(names = SKIP_AMI_TAG_CHECK_ARG,
            description = SKIP_AMI_TAG_CHECK_DESCRIPTION)
    private boolean skipAmiTagCheck;

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final AmiTagCheckService amiTagCheckService;

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Inject
    public CreateCmsClusterCommand(final EnvironmentMetadata environmentMetadata,
                                   final CloudFormationService cloudFormationService,
                                   final Ec2UserDataService ec2UserDataService,
                                   final AmiTagCheckService amiTagCheckService,
                                   final ConfigStore configStore,
                                   @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudFormationObjectMapper) {
        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.amiTagCheckService = amiTagCheckService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void execute() {
        String environmentName = environmentMetadata.getName();
        final String stackName = String.format("%s-%s", environmentName, STACK_NAME);
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final Optional<String> cmsServerCertificateArn = configStore.getServerCertificateArn(StackName.CMS);
        final Optional<String> pubKey = configStore.getCertPart(StackName.CMS, ConfigConstants.CERT_PART_PUBKEY);

        if (!cmsServerCertificateArn.isPresent() || !pubKey.isPresent()) {
            throw new IllegalStateException("CMS certificate has not been uploaded!");
        }

        // Make sure the given AmiId is for CMS component. Check if it contains required tag
        if ( ! skipAmiTagCheck ) {
            amiTagCheckService.validateAmiTagForStack(stackDelegate.getAmiId(), StackName.CMS);
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
                .setHostedZoneId(baseOutputs.getVpcHostedZoneId());

        cmsParameters.getSslConfigParameters().setCertPublicKey(pubKey.get());
        cmsParameters.getSslConfigParameters().setSslCertificateArn(cmsServerCertificateArn.get());

        cmsParameters.getLaunchConfigParameters().setAmiId(stackDelegate.getAmiId());
        cmsParameters.getLaunchConfigParameters().setInstanceSize(stackDelegate.getInstanceSize());
        cmsParameters.getLaunchConfigParameters().setKeyPairName(stackDelegate.getKeyPairName());
        cmsParameters.getLaunchConfigParameters().setUserData(
                ec2UserDataService.getUserData(StackName.CMS, stackDelegate.getOwnerGroup()));

        cmsParameters.getTagParameters().setTagEmail(stackDelegate.getOwnerEmail());
        cmsParameters.getTagParameters().setTagName(ConfigConstants.ENV_PREFIX + environmentMetadata.getName());
        cmsParameters.getTagParameters().setTagCostcenter(stackDelegate.getCostcenter());

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(cmsParameters, typeReference);

        final String stackId = cloudFormationService.createStack(cloudFormationService.getEnvStackName(stackName),
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
    public boolean isRunnable() {
        return configStore.getCmsEnvConfig().isPresent();
    }
}