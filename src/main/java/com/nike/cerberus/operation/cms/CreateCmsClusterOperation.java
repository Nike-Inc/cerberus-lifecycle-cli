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
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
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

    private final ConfigStore configStore;

    private final ObjectMapper cloudFormationObjectMapper;

    @Inject
    public CreateCmsClusterOperation(final EnvironmentMetadata environmentMetadata,
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
    public void run(final CreateCmsClusterCommand command) {
        final String environmentName = environmentMetadata.getName();
        final VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();
        final Optional<String> cmsServerCertificateArn = configStore.getServerCertificateArn(Stack.CMS);
        final Optional<String> pubKey = configStore.getCertPart(Stack.CMS, ConfigConstants.CERT_PART_PUBKEY);

        if (!cmsServerCertificateArn.isPresent() || !pubKey.isPresent()) {
            throw new IllegalStateException("CMS certificate has not been uploaded!");
        }

        // Make sure the given AmiId is for CMS component. Check if it contains required tag
        if ( ! command.isSkipAmiTagCheck() ) {
            amiTagCheckService.validateAmiTagForStack(command.getStackDelegate().getAmiId(), Stack.CMS);
        }

        final CmsParameters cmsParameters = new CmsParameters()
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3())
                .setBaseStackName(Stack.BASE.getFullName(environmentName))
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName))
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName));

        cmsParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        cmsParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        cmsParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        cmsParameters.getLaunchConfigParameters().setUserData(ec2UserDataService.getUserData(Stack.CMS));

        final TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
        final Map<String, String> parameters = cloudFormationObjectMapper.convertValue(cmsParameters, typeReference);

        // allow user to overwrite CloudFormation parameters with -P option
        parameters.putAll(command.getStackDelegate().getDynamicParameters());

        cloudFormationService.createStackAndWait(Stack.CMS,
                parameters, true,
                command.getStackDelegate().getTagParameters().getTags());
    }

    @Override
    public boolean isRunnable(final CreateCmsClusterCommand command) {
        String environmentName = environmentMetadata.getName();

        try {
            cloudFormationService.getStackId(Stack.LOAD_BALANCER.getFullName(environmentName));
            cloudFormationService.getStackId(Stack.SECURITY_GROUPS.getFullName(environmentName));
            cloudFormationService.getStackId(Stack.BASE.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("Could not create the CMS cluster." +
                    "Make sure the load balancer, security group, and base stacks have all been created.", iae);
            return false;
        }

        return configStore.getCmsEnvConfig().isPresent() &&
                ! cloudFormationService.isStackPresent(Stack.CMS.getFullName(environmentName));
    }
}
