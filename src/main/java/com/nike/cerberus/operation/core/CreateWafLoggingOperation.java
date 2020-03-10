/*
 * Copyright (c) 2020 Nike, Inc.
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

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.core.CreateWafLoggingCommand;
import com.nike.cerberus.domain.cloudformation.WafLoggingOutputs;
import com.nike.cerberus.domain.cloudformation.WafLoggingParameters;
import com.nike.cerberus.domain.cloudformation.WafOutputs;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.WafService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Creates WAF logging via CloudFormation.
 */
public class CreateWafLoggingOperation implements Operation<CreateWafLoggingCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final ConfigStore configStore;

    private final WafService wafService;

    private final String webAclArnTemplate = "arn:aws:waf-regional:%s:%s:webacl/%s";

    @Inject
    public CreateWafLoggingOperation(@Named(ENV_NAME) String environmentName,
                                     CloudFormationService cloudFormationService,
                                     CloudFormationObjectMapper cloudFormationObjectMapper,
                                     ConfigStore configStore,
                                     WafService wafService) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.configStore = configStore;
        this.wafService = wafService;
    }

    @Override
    public void run(CreateWafLoggingCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());

        if (!command.isSkipStackCreation()) {
            WafLoggingParameters wafLoggingParameters = new WafLoggingParameters()
                    .setEnvironmentName(environmentName);

            Map<String, String> parameters = cloudFormationObjectMapper.convertValue(wafLoggingParameters);

            cloudFormationService.createStackAndWait(
                    region,
                    Stack.WAF_LOGGING,
                    parameters,
                    true,
                    command.getCloudFormationParametersDelegate().getTags()
            );
        }

        WafLoggingOutputs wafLoggingOutputs = configStore.getStackOutputs(region,
                Stack.WAF_LOGGING.getFullName(environmentName), WafLoggingOutputs.class);
        WafOutputs wafOutputs =
                configStore.getStackOutputs(region,
                        Stack.WAF.getFullName(environmentName), WafOutputs.class);
        String webAclId = wafOutputs.getWebAclID();
        String webAclArn = String.format(webAclArnTemplate, region.getName(), configStore.getAccountId(), webAclId);

        wafService.enableWafLogging(wafLoggingOutputs.getKinesisFirehoseDeliveryStreamARN(), webAclArn, region);
    }

    @Override
    public boolean isRunnable(CreateWafLoggingCommand command) {
        Regions region = command.getCloudFormationParametersDelegate().getStackRegion()
                .orElse(configStore.getPrimaryRegion());
        try {
            cloudFormationService.getStackId(region, Stack.WAF.getFullName(environmentName));
        } catch (IllegalArgumentException iae) {
            logger.error("The web application firewall stack must exist to enable logging!");
            return false;
        }

        return true;
    }
}
