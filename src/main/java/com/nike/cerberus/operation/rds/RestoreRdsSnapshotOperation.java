/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.operation.rds;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.rds.RestoreRdsSnapshotCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.RdsService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for CopyRdsSnapshotsCommand
 *
 * Copies rds cluster snapshots in the primary for this environment to the secondary regions
 */
public class RestoreRdsSnapshotOperation implements Operation<RestoreRdsSnapshotCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final String environmentName;
    private final RdsService rdsService;
    private final CloudFormationService cloudFormationService;
    private final String DESIRED_STATE = "available";
    private final String COPYING_STATE = "copying";

    @Inject
    public RestoreRdsSnapshotOperation(ConfigStore configStore,
                                       @Named(ENV_NAME) String environmentName,
                                       RdsService rdsService,
                                       CloudFormationService cloudFormationService) {

        this.configStore = configStore;
        this.environmentName = environmentName;
        this.rdsService = rdsService;
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    public void run(RestoreRdsSnapshotCommand command) {
        Regions region = Regions.fromName(command.getRegion());
        String stackId = Stack.DATABASE.getFullName(environmentName);
        String databasePassword;

        databasePassword = configStore.getCmsDatabasePassword()
                .orElseThrow(() -> new RuntimeException("Expected the database password to exists before a secondary stack was created!"));

        Map<String, String> parameters = cloudFormationService.getStackParameters(region, stackId);
        parameters.put("snapshotIdentifier", command.getSnapshotIdentifier());
        parameters.put("cmsDbMasterPassword", databasePassword);

        try {
            logger.info("Starting the tags update for '{}'.", stackId);

            cloudFormationService.updateStackAndWait(
                    region,
                    Stack.DATABASE,
                    parameters,
                    true,
                    false,
                    null
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
    public boolean isRunnable(RestoreRdsSnapshotCommand command) {
        boolean isRunnable = true;
        if (StringUtils.isBlank(command.getRegion())){
            logger.error("--region parameter or RESTORE_RDS_SNAPSHOT_REGION environment variable must be set");
            isRunnable = false;
        }
        if (StringUtils.isBlank(command.getRegion())){
            logger.error("--snapshot-identifier parameter or RESTORE_RDS_SNAPSHOT_IDENTIFIER environment variable must be set");
            isRunnable = false;
        }
        if (isRunnable && !cloudFormationService.isStackPresent(Regions.fromName(command.getRegion()),
                Stack.DATABASE.getFullName(environmentName))) {
            logger.error("The Database stack must exist in the target region in order to restore snapshots");
            isRunnable = false;
        }

        return isRunnable;
    }
}
