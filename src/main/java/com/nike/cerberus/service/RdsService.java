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

package com.nike.cerberus.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CopyDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DeleteDBClusterSnapshotRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.amazonaws.services.rds.model.Tag;
import com.nike.cerberus.operation.UnexpectedRdsSnapshotStatusException;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class RdsService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AwsClientFactory<AmazonRDSClient> amazonRDSClientFactory;
    private final ConfigStore configStore;
    private final String environmentName;
    private static final String DESIRED_STATE = "available";
    private static final String COPYING_STATE = "copying";
    private static final String CREATING_STATE = "creating";

    @Inject
    public RdsService(AwsClientFactory<AmazonRDSClient> amazonRDSClientFactory,
                      ConfigStore configStore,
                      @Named(ENV_NAME) String environmentName) {

        this.amazonRDSClientFactory = amazonRDSClientFactory;
        this.configStore = configStore;
        this.environmentName = environmentName;
    }

    /**
     * Copies a snapshot from one region to another
     * @param fromSnapshot The snapshot that is getting copied
     * @param fromRegion The from region
     * @param toRegion The to region
     */
    public DBClusterSnapshot copySnapshot(DBClusterSnapshot fromSnapshot, Regions fromRegion, Regions toRegion) {
        AmazonRDS rds = amazonRDSClientFactory.getClient(toRegion);
        DBClusterSnapshot dbClusterSnapshot = rds.copyDBClusterSnapshot(new CopyDBClusterSnapshotRequest()
                .withCopyTags(true)
                .withSourceDBClusterSnapshotIdentifier(fromSnapshot.getDBClusterSnapshotArn())
                .withTargetDBClusterSnapshotIdentifier(getIdentifier(fromSnapshot))
                .withSourceRegion(fromRegion.getName())
                .withKmsKeyId(configStore.getEnvironmentData().getRegionData().get(toRegion).getConfigCmkArn().get())
                .withTags(
                        new Tag().withKey("created_by").withValue("cerberus_cli")
                )
        );
        return dbClusterSnapshot;
    }

    /**
     * Strips the rds: that aws prepends to automatic snapshots
     * @param dbSnapshot the snapshot
     * @return a string that can be used to identify automatic and manual copies rds:foo will return foo
     */
    public String getIdentifier(DBClusterSnapshot dbSnapshot) {
        return dbSnapshot.getDBClusterSnapshotIdentifier().replace("rds:", "");
    }

    /**
     * @return True if the snapshot was created for the cms database cluster for this environment
     */
    public boolean wasSnapshotGeneratedFromCmsCluster(DBClusterSnapshot dbSnapshot) {
        String identifier = getIdentifier(dbSnapshot);
        return identifier.startsWith(environmentName) && identifier.contains("cmsdatabasecluster");
    }

    /**
     * @param dbSnapshot The snapshot under question
     * @param days How many days to compare to
     * @return True if the snapshot under question is newer than the days passed in
     */
    public boolean isSnapshotNewerThanGivenDays(DBClusterSnapshot dbSnapshot, long days) {
        return ZonedDateTime.ofInstant(dbSnapshot.getSnapshotCreateTime().toInstant(), ZoneId.of("UTC"))
                .isAfter(Instant.now().atZone(ZoneId.of("UTC")).minus(days, ChronoUnit.DAYS));
    }

    /**
     * @return All the RDS Cluster Snapshots for a region
     */
    public List<DBClusterSnapshot> getDbSnapshots(Regions region) {
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

    /**
     * Deletes a RDS DB Cluster Snapshot in a given region.
     *
     * @param snapshot The snapshot to delete
     * @param region The region that the snapshot is in
     */
    public void deleteSnapshot(DBClusterSnapshot snapshot, Regions region) {
        log.info("preparing to delete snapshot: {} with creation date: {} in region: {}",
                snapshot.getDBClusterSnapshotIdentifier(), snapshot.getSnapshotCreateTime(), region);

        amazonRDSClientFactory.getClient(region).deleteDBClusterSnapshot(new DeleteDBClusterSnapshotRequest()
                .withDBClusterSnapshotIdentifier(snapshot.getDBClusterSnapshotIdentifier()));
    }

    /**
     * Wait for a single RDS cluster snapshots to become available.
     *
     * @param snapshot The snapshot to query status
     * @param region The region that the snapshot is in
     */
    public void waitForSnapshotsToBecomeAvailable(DBClusterSnapshot snapshot, Regions region) {
        Deque<DBClusterSnapshot> snapshots = new LinkedList<>();
        snapshots.offer(snapshot);
        Deque<Regions> regions = new LinkedList<>();
        regions.offer(region);
        waitForSnapshotsToBecomeAvailable(snapshots, regions);
    }

    /**
     * Wait for a Deque of RDS cluster snapshots to become available in a round robin fashion.
     *
     * @param snapshots The snapshots to query status
     * @param regions The regions that the snapshots are in. The order of regions should match the snapshots'
     */
    public void waitForSnapshotsToBecomeAvailable(Deque<DBClusterSnapshot> snapshots, Deque<Regions> regions) {
        while (!snapshots.isEmpty()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            DBClusterSnapshot snapshot = snapshots.poll();
            Regions region = regions.poll();
            String snapshotIdentifier = snapshot.getDBClusterSnapshotIdentifier();
            String status = getSnapshotStatus(snapshotIdentifier, region);

            if (DESIRED_STATE.equals(status)) {
                log.info("RDS cluster snapshot {} in region {} is available.", snapshotIdentifier, region.getName());
            } else if (COPYING_STATE.equals(status) || CREATING_STATE.equals(status)){
                log.info("Waiting for RDS cluster snapshot {} in region {} to become available.",
                        snapshotIdentifier, region.getName());
                snapshots.offer(snapshot);
                regions.offer(region);
            } else {
                throw new UnexpectedRdsSnapshotStatusException(
                        String.format("RDS cluster snapshot %s in region: %s is in undesired status: %s",
                                snapshotIdentifier, region.getName(), status));
            }
        }
        log.info("All RDS snapshots are available.");
    }

    /**
     * Creates a RDS DB Cluster Snapshot in a given region.
     *
     * @param clusterIdentifer The identifier of RDS cluster to take snapshot of
     * @param region The region the RDS cluster is in
     */
    public DBClusterSnapshot createSnapshot(String clusterIdentifer, Regions region) {
        AmazonRDS rds = amazonRDSClientFactory.getClient(region);
        String snapshotIdentifier = generateSnapshotIdentifier(clusterIdentifer, LocalDateTime.now());
        CreateDBClusterSnapshotRequest request = new CreateDBClusterSnapshotRequest()
                .withDBClusterIdentifier(clusterIdentifer)
                .withDBClusterSnapshotIdentifier(snapshotIdentifier);
        DBClusterSnapshot dbClusterSnapshot = rds.createDBClusterSnapshot(request);
        return dbClusterSnapshot;
    }

    private String getSnapshotStatus(String snapshotIdentifier, Regions region) {
        AmazonRDS rds = amazonRDSClientFactory.getClient(region);
        DescribeDBClusterSnapshotsRequest request = new DescribeDBClusterSnapshotsRequest()
                .withDBClusterSnapshotIdentifier(snapshotIdentifier);

        DescribeDBClusterSnapshotsResult result = rds.describeDBClusterSnapshots(request);

        if (result.getDBClusterSnapshots().size() > 0) {
            String status = result.getDBClusterSnapshots().get(0).getStatus();

            if (StringUtils.isNotBlank(status)) {
                return status;
            }
        }

        return null;
    }

    protected String generateSnapshotIdentifier(String clusterIdentifer, LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        String formatDateTime = localDateTime.format(formatter);
        return clusterIdentifer + "-" + formatDateTime;
    }
}
