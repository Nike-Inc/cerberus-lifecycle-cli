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

import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for creating the CMS cluster.
 */
public class CreateCmsClusterOperation implements Operation<CreateCmsClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final Ec2UserDataService ec2UserDataService;

    private final ConfigStore configStore;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final String environmentName;

    @Inject
    public CreateCmsClusterOperation(CloudFormationService cloudFormationService,
                                     Ec2UserDataService ec2UserDataService,
                                     ConfigStore configStore,
                                     CloudFormationObjectMapper cloudFormationObjectMapper,
                                     @Named(ENV_NAME) String environmentName) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.configStore = configStore;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
    }

    @Override
    public void run(CreateCmsClusterCommand command) {
        VpcOutputs vpcOutputs = configStore.getVpcStackOutputs();
        List<CertificateInformation> certInfoListForStack = configStore.getCertificationInformationList();

        Map<String, String> tags = command.getStackDelegate().getTagParameters().getTags();

        if (certInfoListForStack.isEmpty()) {
            throw new IllegalStateException("Certificate for cerberus environment has not been uploaded!");
        }

        CmsParameters cmsParameters = new CmsParameters()
                .setVpcSubnetIdForAz1(vpcOutputs.getVpcSubnetIdForAz1())
                .setVpcSubnetIdForAz2(vpcOutputs.getVpcSubnetIdForAz2())
                .setVpcSubnetIdForAz3(vpcOutputs.getVpcSubnetIdForAz3())
                .setBaseStackName(Stack.IAM_ROLES.getFullName(environmentName))
                .setLoadBalancerStackName(Stack.LOAD_BALANCER.getFullName(environmentName))
                .setSgStackName(Stack.SECURITY_GROUPS.getFullName(environmentName));

        cmsParameters.getLaunchConfigParameters().setAmiId(command.getStackDelegate().getAmiId());
        cmsParameters.getLaunchConfigParameters().setInstanceSize(command.getStackDelegate().getInstanceSize());
        cmsParameters.getLaunchConfigParameters().setKeyPairName(command.getStackDelegate().getKeyPairName());
        cmsParameters.getLaunchConfigParameters().setUserData(ec2UserDataService.getUserData(
                configStore.getPrimaryRegion(), Stack.CMS,
                Optional.ofNullable(tags.getOrDefault("ownerGroup", null))));

        Map<String, String> parameters = cloudFormationObjectMapper.convertValue(cmsParameters);

        // allow user to overwrite CloudFormation parameters with -P option
        parameters.putAll(command.getStackDelegate().getDynamicParameters());

        cloudFormationService.createStackAndWait(
                configStore.getPrimaryRegion(),
                Stack.CMS,
                parameters, true,
                tags);
    }

    @Override
    public boolean isRunnable(CreateCmsClusterCommand command) {
        try {
            cloudFormationService.getStackId(configStore.getPrimaryRegion(), Stack.LOAD_BALANCER.getFullName(environmentName));
            cloudFormationService.getStackId(configStore.getPrimaryRegion(), Stack.SECURITY_GROUPS.getFullName(environmentName));
            cloudFormationService.getStackId(configStore.getPrimaryRegion(), Stack.IAM_ROLES.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("Could not create the CMS cluster." +
                    "Make sure the load balancer, security group, and base stacks have all been created.", iae);
            return false;
        }

        return configStore.getCmsEnvConfig().isPresent() &&
                !cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.CMS.getFullName(environmentName));
    }
}
