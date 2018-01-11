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

package com.nike.cerberus.operation.rds;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.RdsService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.stream.Collectors;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for CopyRdsSnapshotsCommand
 *
 * Copies rds cluster snapshots in the primary for this environment to the secondary regions
 */
public class CopyRdsSnapshotsOperation implements Operation<CopyRdsSnapshotsCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final String environmentName;
    private final RdsService rdsService;
    private final CloudFormationService cloudFormationService;

    @Inject
    public CopyRdsSnapshotsOperation(ConfigStore configStore,
                                     @Named(ENV_NAME) String environmentName,
                                     RdsService rdsService,
                                     CloudFormationService cloudFormationService) {

        this.configStore = configStore;
        this.environmentName = environmentName;
        this.rdsService = rdsService;
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    public void run(CopyRdsSnapshotsCommand command) {
        Regions primaryRegion = configStore.getPrimaryRegion();
        List<DBClusterSnapshot> snapshotsInPrimaryRegion = rdsService.getDbSnapshots(primaryRegion).stream()
                .filter(dbSnapshot ->
                        rdsService.wasSnapshotGeneratedFromCmsCluster(dbSnapshot) && rdsService.isSnapshotNewerThanGivenDays(dbSnapshot, command.getDays())
                ).collect(Collectors.toList());

        configStore.getConfigEnabledRegions().forEach(region -> {
            if (region.equals(primaryRegion)) {
                return;
            }

            List<DBClusterSnapshot> toRegionSnapshots = rdsService.getDbSnapshots(region).stream()
                    .filter(rdsService::wasSnapshotGeneratedFromCmsCluster)
                    .collect(Collectors.toList());

            snapshotsInPrimaryRegion.forEach(fromSnapshot -> {
                boolean needsCopying = toRegionSnapshots.stream().noneMatch(toSnapshot ->
                        rdsService.getIdentifier(fromSnapshot).equals(rdsService.getIdentifier(toSnapshot)));
                if (needsCopying) {
                    log.info("Preparing to initiate copy of RDS DB Snapshot: {} located in region: {} to region: {}",
                            fromSnapshot.getDBClusterSnapshotIdentifier(), primaryRegion, region);

                    rdsService.copySnapshot(fromSnapshot, primaryRegion, region);
                } else {
                    log.info("Snapshot: {} already copied to region: {}, skipping...", rdsService.getIdentifier(fromSnapshot), region);
                }
            });
        });
        log.info("Finished issuing copy api calls to aws, it can take a long time for copies to finish.");
    }



    @Override
    public boolean isRunnable(CopyRdsSnapshotsCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.DATABASE.getFullName(environmentName))) {
            log.error("The Database stuck must exist in the primary region in order to duplicate snapshots");
            isRunnable = false;
        }

        return isRunnable;
    }
}
