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
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CopyDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.Tag;
import com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
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
    private final AwsClientFactory<AmazonRDSClient> amazonRDSClientFactory;
    private final CloudFormationService cloudFormationService;

    @Inject
    public CopyRdsSnapshotsOperation(ConfigStore configStore,
                                     @Named(ENV_NAME) String environmentName,
                                     AwsClientFactory<AmazonRDSClient> amazonRDSClientFactory,
                                     CloudFormationService cloudFormationService) {

        this.configStore = configStore;
        this.environmentName = environmentName;
        this.amazonRDSClientFactory = amazonRDSClientFactory;
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    public void run(CopyRdsSnapshotsCommand command) {
        Regions primaryRegion = configStore.getPrimaryRegion();
        List<DBClusterSnapshot> snapshotsInPrimaryRegion = getDbSnapshots(primaryRegion).stream()
                .filter(dbSnapshot ->
                        wasSnapshotGeneratedFromCmsCluster(dbSnapshot) && isSnapshotNewerThanGivenDays(dbSnapshot, command.getDays())
                ).collect(Collectors.toList());

        configStore.getConfigEnabledRegions().forEach(region -> {
            if (region.equals(primaryRegion)) {
                return;
            }

            List<DBClusterSnapshot> toRegionSnapshots = getDbSnapshots(region).stream()
                    .filter(this::wasSnapshotGeneratedFromCmsCluster)
                    .collect(Collectors.toList());

            snapshotsInPrimaryRegion.forEach(fromSnapshot -> {
                boolean needsCopying = toRegionSnapshots.stream().noneMatch(toSnapshot ->
                        getIdentifier(fromSnapshot).equals(getIdentifier(toSnapshot)));
                if (needsCopying) {
                    log.info("Preparing to initiate copy of RDS DB Snapshot: {} located in region: {} to region: {}",
                            fromSnapshot.getDBClusterSnapshotIdentifier(), primaryRegion, region);

                    copySnapshot(fromSnapshot, primaryRegion, region);
                } else {
                    log.info("Snapshot: {} already copied to region: {}, skipping...", getIdentifier(fromSnapshot), region);
                }
            });
        });
        log.info("Finished issuing copy api calls to aws, it can take a long time for copies to finish.");
    }

    /**
     * Copies a snapshot from one region to another
     * @param fromSnapshot The snapshot that is getting copied
     * @param fromRegion The from region
     * @param toRegion The to region
     */
    private void copySnapshot(DBClusterSnapshot fromSnapshot, Regions fromRegion, Regions toRegion) {
        AmazonRDS rds = amazonRDSClientFactory.getClient(toRegion);
        rds.copyDBClusterSnapshot(new CopyDBClusterSnapshotRequest()
                .withCopyTags(true)
                .withSourceDBClusterSnapshotIdentifier(fromSnapshot.getDBClusterSnapshotArn())
                .withTargetDBClusterSnapshotIdentifier(getIdentifier(fromSnapshot))
                .withSourceRegion(fromRegion.getName())
                .withKmsKeyId(configStore.getEnvironmentData().getRegionData().get(toRegion).getConfigCmkArn().get())
                .withTags(
                        new Tag().withKey("created_by").withValue("cerberus_cli")
                )
        );
    }

    /**
     * Strips the rds: that aws prepends to automatic snapshots
     * @param dbSnapshot the snapshot
     * @return a string that can be used to identify automatic and manual copies rds:foo will return foo
     */
    protected String getIdentifier(DBClusterSnapshot dbSnapshot) {
        return dbSnapshot.getDBClusterSnapshotIdentifier().replace("rds:", "");
    }

    /**
     * @return True if the snapshot was created for the cms database cluster for this environment
     */
    protected boolean wasSnapshotGeneratedFromCmsCluster(DBClusterSnapshot dbSnapshot) {
        String identifier = getIdentifier(dbSnapshot);
        return identifier.startsWith(environmentName) && identifier.contains("cmsdatabasecluster");
    }

    /**
     * @param dbSnapshot The snapshot under question
     * @param days How many days to compare to
     * @return True if the snapshot under question is newer than the days passed in
     */
    protected boolean isSnapshotNewerThanGivenDays(DBClusterSnapshot dbSnapshot, long days) {
        return ZonedDateTime.ofInstant(dbSnapshot.getSnapshotCreateTime().toInstant(), ZoneId.of("UTC"))
                .isAfter(Instant.now().atZone(ZoneId.of("UTC")).minus(days, ChronoUnit.DAYS));
    }

    /**
     * @return All the RDS Cluster Snapshots for a region
     */
    protected List<DBClusterSnapshot> getDbSnapshots(Regions region) {
        AmazonRDS rds = amazonRDSClientFactory.getClient(region);
        List<DBClusterSnapshot> cmsDatabaseClusterSnapshots = new LinkedList<>();
        String next = null;
        do {
            DescribeDBClusterSnapshotsRequest req = new DescribeDBClusterSnapshotsRequest();

            if (next != null) {
                req.withMarker(next);
            }

            DescribeDBClusterSnapshotsResult res = rds.describeDBClusterSnapshots(req);

            cmsDatabaseClusterSnapshots.addAll(res.getDBClusterSnapshots());
            next = res.getMarker();
        } while (next != null);

        return cmsDatabaseClusterSnapshots;
    }

    @Override
    public boolean isRunnable(CopyRdsSnapshotsCommand command) {
        boolean isRunnable = true;

        if (cloudFormationService.isStackPresent(configStore.getPrimaryRegion(),
                Stack.DATABASE.getFullName(environmentName))) {
            log.error("The Database stuck must exist in the primary region in order to duplicate snapshots");
            isRunnable = false;
        }

        return isRunnable;
    }
}
