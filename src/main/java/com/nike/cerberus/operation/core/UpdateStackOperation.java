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

import com.amazonaws.AmazonServiceException;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AmiTagCheckService;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for updating stacks.
 */
public class UpdateStackOperation implements Operation<UpdateStackCommand> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final Ec2UserDataService ec2UserDataService;
    private final String environmentName;
    private final ConfigStore configStore;
    private final AmiTagCheckService amiTagCheckService;

    @Inject
    public UpdateStackOperation(CloudFormationService cloudFormationService,
                                Ec2UserDataService ec2UserDataService,
                                @Named(ENV_NAME) String environmentName,
                                AmiTagCheckService amiTagCheckService,
                                ConfigStore configStore) {

        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.environmentName = environmentName;
        this.amiTagCheckService = amiTagCheckService;
        this.configStore = configStore;
    }

    @Override
    public void run(UpdateStackCommand command) {
        String stackId = command.getStack().getFullName(environmentName);

        Map<String, String> parameters = cloudFormationService.getStackParameters(configStore.getPrimaryRegion(), stackId);
        Map<String, String> tags = command.getTagsDelegate().getTags();

        // only some stacks need user data
        if (command.getStack().needsUserData()) {
            parameters.put("userData", ec2UserDataService.getUserData(configStore.getPrimaryRegion(), command.getStack(),
                    Optional.ofNullable(tags.getOrDefault("ownerGroup", null))));
        }

        if (Stack.CMS.equals(command.getStack())) {
            command.getDynamicParameters().forEach((key, value) -> {
                if (key.equals("amiId")) {
                    amiTagCheckService.validateAmiTagForStack(value, Stack.CMS);
                }
            });
        } else if (Stack.DATABASE.equals(command.getStack())) {
            Optional<String> dbPasswordOverwrite = command.getDynamicParameters().entrySet().stream()
                    .filter(entry -> entry.getKey().equals("cmsDbMasterPassword"))
                    .map(Map.Entry::getValue)
                    .findFirst();

            dbPasswordOverwrite.ifPresent(configStore::storeCmsDatabasePassword);

            parameters.put("cmsDbMasterPassword", dbPasswordOverwrite.orElseGet(() ->
                    configStore.getCmsDatabasePassword().orElseThrow(() ->
                    new RuntimeException("Unable to find current database password, add new one " +
                            "with -PcmsDbMasterPassword=xxxxxxx"))));

        } else if (Stack.LOAD_BALANCER.equals(command.getStack())) {
            parameters.put("sslCertificateArn", configStore.getCertificationInformationList()
                    .getLast().getIdentityManagementCertificateArn());
        }
        parameters.putAll(command.getDynamicParameters());

        try {
            logger.info("Starting the update for '{}' overwrite:{}.", stackId, command.isOverwriteTemplate());

            cloudFormationService.updateStackAndWait(
                    configStore.getPrimaryRegion(),
                    command.getStack(),
                    parameters,
                    true,
                    command.isOverwriteTemplate(),
                    command.getTagsDelegate().getTags()
            );

            logger.info("Update complete.");
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == 400 &&
                    StringUtils.equalsIgnoreCase(ase.getErrorMessage(), "No updates are to be performed.")) {
                logger.warn("CloudFormation reported no changes detected.");
            } else {
                throw ase;
            }
        }
    }

    @Override
    public boolean isRunnable(UpdateStackCommand command) {
        configStore.isConfigSynchronized();
        boolean isRunnable = true;

        String fullName = command.getStack().getFullName(environmentName);
        if (!cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), fullName)) {
            logger.error("CloudFormation doesn't have the specified stack: {}", fullName);
            isRunnable = false;
        }

        if (command.getStack().equals(Stack.LOAD_BALANCER) && configStore.getCertificationInformationList().isEmpty()) {
            logger.error("Updating the load balancer requires that a cert has been uploaded by the upload-certificate-files command");
            isRunnable = false;
        }

        return isRunnable;
    }
}