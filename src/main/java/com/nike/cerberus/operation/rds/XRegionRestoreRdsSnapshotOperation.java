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
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.google.common.collect.ImmutableList;
import com.nike.cerberus.command.rds.XRegionRestoreRdsSnapshotCommand;
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
import java.util.List;
import java.util.Map;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;
import static com.google.common.collect.MoreCollectors.onlyElement;

/**
 * Operation for XRegionRestoreRdsSnapshotCommand
 *
 * Restores rds cluster in the target region for this environment from a fresh RDS cluster snapshot created in the source region
 */
public class XRegionRestoreRdsSnapshotOperation implements Operation<XRegionRestoreRdsSnapshotCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final String environmentName;
    private final RdsService rdsService;
    private final CloudFormationService cloudFormationService;

    @Inject
    public XRegionRestoreRdsSnapshotOperation(ConfigStore configStore,
                                              @Named(ENV_NAME) String environmentName,
                                              RdsService rdsService,
                                              CloudFormationService cloudFormationService) {

        this.configStore = configStore;
        this.environmentName = environmentName;
        this.rdsService = rdsService;
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    public void run(XRegionRestoreRdsSnapshotCommand command) {
        Regions sourceRegion = Regions.fromName(command.getSourceRegion());
        Regions targetRegion = Regions.fromName(command.getTargetRegion());

        String stackName = Stack.DATABASE.getFullName(environmentName);
        List<StackResourceSummary> stackResources = cloudFormationService.getStackResources(sourceRegion, stackName);
        StackResourceSummary sourceDbCluster = stackResources
                .stream()
                .filter(resource -> "CmsDatabaseCluster".equals(resource.getLogicalResourceId()))
                .collect(onlyElement());
        String sourceDbClusterId = sourceDbCluster.getPhysicalResourceId();

        log.info("Preparing to create snapshot of RDS cluster in region: {}", sourceRegion);
        DBClusterSnapshot sourceSnapshot = rdsService.createSnapshot(sourceDbClusterId, sourceRegion);
        rdsService.waitForSnapshotsToBecomeAvailable(sourceSnapshot, sourceRegion);

        log.info("Preparing to initiate copy of RDS DB snapshot: {} located in region: {} to region: {}",
                    sourceSnapshot.getDBClusterSnapshotIdentifier(), sourceRegion.getName(), targetRegion.getName());
        DBClusterSnapshot copiedSnapshot = rdsService.copySnapshot(sourceSnapshot, sourceRegion, targetRegion);
        rdsService.waitForSnapshotsToBecomeAvailable(copiedSnapshot, targetRegion);

        rdsService.deleteSnapshot(sourceSnapshot, sourceRegion);

        String databasePassword = configStore.getCmsDatabasePassword()
                .orElseThrow(() -> new RuntimeException("Expected the database password to exist"));
        Map<String, String> parameters = cloudFormationService.getStackParameters(targetRegion, stackName);
        parameters.put("snapshotIdentifier", copiedSnapshot.getDBClusterSnapshotIdentifier());
        parameters.put("cmsDbMasterPassword", databasePassword);

        try {
            log.info("Preparing to initiate restore of RDS DB snapshot {} in region {}",
                    copiedSnapshot.getDBClusterSnapshotIdentifier(),
                    targetRegion);

            cloudFormationService.updateStackAndWait(
                    targetRegion,
                    Stack.DATABASE,
                    parameters,
                    true,
                    false,
                    null,
                    true);

            log.info("Restore complete.");
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == 400 &&
                    StringUtils.equalsIgnoreCase(ase.getErrorMessage(), "No updates are to be performed.")) {
                log.warn("CloudFormation reported no changes detected.");
            } else {
                throw ase;
            }
        }

        rdsService.deleteSnapshot(copiedSnapshot, targetRegion);
    }



    @Override
    public boolean isRunnable(XRegionRestoreRdsSnapshotCommand command) {
        boolean isRunnable = true;
        Regions targetRegion = Regions.fromName(command.getTargetRegion());
        Regions sourceRegion = Regions.fromName(command.getSourceRegion());

        ImmutableList<Regions> regions = ImmutableList.of(targetRegion, sourceRegion);
        if (!configStore.getCmsRegions().containsAll(regions)) {
            log.error("The source and target regions must be configured for the environment");
            isRunnable = false;
        }

        if (isRunnable && !cloudFormationService.isStackPresent(targetRegion, Stack.DATABASE.getFullName(environmentName))) {
            log.error("The Database stack must exist in the target region in order to restore snapshot");
            isRunnable = false;
        }

        return isRunnable;
    }
}
