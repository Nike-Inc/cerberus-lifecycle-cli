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

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.rds.CleanUpRdsSnapshotsCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.RdsService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class CleanUpRdsSnapshotsOperation implements Operation<CleanUpRdsSnapshotsCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final String environmentName;
    private final RdsService rdsService;
    private final CloudFormationService cloudFormationService;

    @Inject
    public CleanUpRdsSnapshotsOperation(ConfigStore configStore,
                                        @Named(ENV_NAME) String environmentName,
                                        RdsService rdsService,
                                        CloudFormationService cloudFormationService) {

        this.configStore = configStore;
        this.environmentName = environmentName;
        this.rdsService = rdsService;
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    public void run(CleanUpRdsSnapshotsCommand command) {
        Regions primaryRegion = configStore.getPrimaryRegion();
        Date oldestAcceptableSnapshotCreationDate = Date.from(Instant.now().minus(command.getDays(), ChronoUnit.DAYS));

        // Go through each config region
        configStore.getConfigEnabledRegions().stream()
                // and filter out the primary region, because rds cleans those snapshots automatically
                .filter(region -> ! region.equals(primaryRegion))
                .forEach(region ->
                        // in each region list all the snapshots
                        rdsService.getDbSnapshots(region).stream()
                        // filter out snapshots that are not from the cluster / environment under question
                        // and are older than the number of acceptable days
                        .filter(snapshot ->
                                rdsService.wasSnapshotGeneratedFromCmsCluster(snapshot) &&
                                snapshot.getSnapshotCreateTime().before(oldestAcceptableSnapshotCreationDate))
                        // delete the snapshots
                        .forEach(snapshot -> {
                            if (command.isDryRun()) {
                                log.info("snapshot: {} with creation date: {} in region: {}",
                                        snapshot.getDBClusterSnapshotIdentifier(), snapshot.getSnapshotCreateTime(), region);
                            } else {
                                rdsService.deleteSnapshot(snapshot, region);
                            }
                        }));
    }

    @Override
    public boolean isRunnable(CleanUpRdsSnapshotsCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.DATABASE.getFullName(environmentName))) {
            log.error("The Database stuck must exist in the primary region in order to have snapshots to clean up");
            isRunnable = false;
        }

        return isRunnable;
    }
}
