/*
 * Copyright (c) 2018 Nike, Inc.
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
import com.nike.cerberus.command.core.UpdateStackTagsCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for updating stack tags.
 */
public class UpdateStackTagsOperation implements Operation<UpdateStackTagsCommand> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final String environmentName;
    private final ConfigStore configStore;

    @Inject
    public UpdateStackTagsOperation(CloudFormationService cloudFormationService,
                                    @Named(ENV_NAME) String environmentName,
                                    ConfigStore configStore) {

        this.cloudFormationService = cloudFormationService;
        this.environmentName = environmentName;
        this.configStore = configStore;
    }

    @Override
    public void run(UpdateStackTagsCommand command) {
        String stackId = command.getStack().getFullName(environmentName);
        Map<String, String> parameters = cloudFormationService.getStackParameters(configStore.getPrimaryRegion(), stackId);
        Map<String, String> tags = command.getTagsDelegate().getTags();
        if (!command.isOverwriteTags()){
            Map<String, String> existingTags = cloudFormationService.getStackTags(configStore.getPrimaryRegion(), stackId);
            existingTags.forEach((k, v) -> tags.merge(k, v, (o, n)->o));
        }
        if (Stack.DATABASE.equals(command.getStack())) {
            parameters.put("cmsDbMasterPassword", configStore.getCmsDatabasePassword().orElseThrow(() ->
                            new RuntimeException("Unable to find current database password, add new one " +
                                    "with -PcmsDbMasterPassword=xxxxxxx")));
        }

        try {
            logger.info("Starting the tags update for '{}'.", stackId);

            cloudFormationService.updateStackAndWait(
                    configStore.getPrimaryRegion(),
                    command.getStack(),
                    parameters,
                    true,
                    false,
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
    public boolean isRunnable(UpdateStackTagsCommand command) {
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