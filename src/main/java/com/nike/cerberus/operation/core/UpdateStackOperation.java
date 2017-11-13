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
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.Ec2UserDataService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;

/**
 * Operation for updating stacks.
 */
public class UpdateStackOperation implements Operation<UpdateStackCommand> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final Ec2UserDataService ec2UserDataService;
    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public UpdateStackOperation(final CloudFormationService cloudFormationService,
                                final Ec2UserDataService ec2UserDataService,
                                final EnvironmentMetadata environmentMetadata) {
        this.cloudFormationService = cloudFormationService;
        this.ec2UserDataService = ec2UserDataService;
        this.environmentMetadata = environmentMetadata;


    }

    @Override
    public void run(final UpdateStackCommand command) {
        final String stackId = command.getStack().getFullName(environmentMetadata.getName());

        final Map<String, String> parameters = cloudFormationService.getStackParameters(stackId);

        // only some stacks need user data
        if (command.getStack().needsUserData()) {
            parameters.put("userData", ec2UserDataService.getUserData(command.getStack()));
        }

        if (Stack.CMS.equals(command.getStack())) {
            // TODO: tag check
        } else if (Stack.DATABASE.equals(command.getStack())) {
            // TODO: implement storing password if it was changed
            //configStore.storeCmsDatabasePassword(databasePassword);
        }
        parameters.putAll(command.getDynamicParameters());

        try {
            logger.info("Starting the update for '{}' overwrite:{}.", stackId, command.isOverwriteTemplate());

            cloudFormationService.updateStack(command.getStack(), parameters, true, command.isOverwriteTemplate(),
                    command.getTagsDelegate().getTags());

            final StackStatus endStatus =
                    cloudFormationService.waitForStatus(stackId,
                            Sets.newHashSet(
                                    UPDATE_COMPLETE,
                                    UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_COMPLETE,
                                    UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
                                    UPDATE_ROLLBACK_FAILED
                            ));

            if (!ImmutableList.of(UPDATE_COMPLETE, UPDATE_COMPLETE_CLEANUP_IN_PROGRESS).contains(endStatus)) {
                final String errorMessage = String.format("CloudFormation reports that updating the stack was not successful. end status: %s", endStatus.name());
                logger.error(errorMessage);

                throw new UnexpectedCloudFormationStatusException(errorMessage);
            }

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
    public boolean isRunnable(final UpdateStackCommand command) {
        boolean isRunnable = true;

        String fullName = command.getStack().getFullName(environmentMetadata.getName());
        if (!cloudFormationService.isStackPresent(fullName)) {
            logger.error("CloudFormation doesn't have the specified stack: {}", fullName);
            isRunnable = false;
        }

        return isRunnable;
    }
}