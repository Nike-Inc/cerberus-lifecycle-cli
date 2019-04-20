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
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult;
import com.nike.cerberus.operation.UnexpectedRdsSnapshotStatusException;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.RdsService;
import com.nike.cerberus.store.ConfigStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RdsServiceTest {

    @Mock
    private ConfigStore configStore;

    private final static String envName = "test";

    private RdsService rdsService;

    @Mock
    private AmazonRDSClient amazonRDSClient;

    @Mock
    private AwsClientFactory<AmazonRDSClient> rdsClientFactory;

    @Before
    public void before() {
        initMocks(this);
        rdsService = new RdsService(rdsClientFactory, configStore, envName);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_true_if_id_matches() {
        boolean actual = rdsService.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:test-cerberus-database-cmsdatabasecluster-jns1lasdf9d-2017-12-12-18-07"));

        assertTrue(actual);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_false_for_a_different_env() {
        boolean actual = rdsService.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:dev-cerberus-database-cmsdatabasecluster-jns1lasdf9d-2017-12-12-18-07"));

        assertFalse(actual);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_false_if_id_does_not_matche() {
        boolean actual = rdsService.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:some-other-db-cluster-jns1lasdf9d-2017-12-12-18-07"));

        assertFalse(actual);
    }

    @Test
    public void test_that_isSnapshotNewerThan24Hours_returns_true_if_snapshot_is_newer_than_24h() {
        DBClusterSnapshot ss = new DBClusterSnapshot().withSnapshotCreateTime(Date.from(Instant.now().atZone(ZoneId.of("UTC")).toInstant()));

        boolean actual = rdsService.isSnapshotNewerThanGivenDays(ss, 1);

        assertTrue(actual);
    }

    @Test
    public void test_that_isSnapshotNewerThan24Hours_returns_false_if_snapshot_is_older_than_24h() {
        DBClusterSnapshot ss = new DBClusterSnapshot().withSnapshotCreateTime(Date.from(Instant.now().atZone(ZoneId.of("UTC"))
                .minus(1, ChronoUnit.WEEKS).toInstant()));

        boolean actual = rdsService.isSnapshotNewerThanGivenDays(ss, 1);

        assertFalse(actual);
    }

    @Test
    public void test_that_wait_for_snapshot_to_become_available_returns_when_snapshot_status_is_available() {
        Deque<DBClusterSnapshot> dbClusterSnapshots = new LinkedList<>();
        dbClusterSnapshots.add(new DBClusterSnapshot().withDBClusterSnapshotIdentifier("ssid").withStatus("available"));
        Deque<Regions> regions = new LinkedList<>();
        regions.add(Regions.US_EAST_1);

        when(rdsClientFactory.getClient(Regions.US_EAST_1)).thenReturn(amazonRDSClient);
        DescribeDBClusterSnapshotsResult result = new DescribeDBClusterSnapshotsResult().withDBClusterSnapshots(dbClusterSnapshots);
        when(amazonRDSClient.describeDBClusterSnapshots(any())).thenReturn(result);

        rdsService.waitForSnapshotsToBecomeAvailable(dbClusterSnapshots, regions);
    }

    @Test(expected = UnexpectedRdsSnapshotStatusException.class)
    public void test_that_wait_for_snapshot_to_become_available_throws_exception_when_snapshot_status_is_unknown() {
        Deque<DBClusterSnapshot> dbClusterSnapshots = new LinkedList<>();
        dbClusterSnapshots.add(new DBClusterSnapshot().withDBClusterSnapshotIdentifier("ssid").withStatus("unknown"));
        Deque<Regions> regions = new LinkedList<>();
        regions.add(Regions.US_EAST_1);

        when(rdsClientFactory.getClient(Regions.US_EAST_1)).thenReturn(amazonRDSClient);
        DescribeDBClusterSnapshotsResult result = new DescribeDBClusterSnapshotsResult().withDBClusterSnapshots(dbClusterSnapshots);
        when(amazonRDSClient.describeDBClusterSnapshots(any())).thenReturn(result);

        rdsService.waitForSnapshotsToBecomeAvailable(dbClusterSnapshots, regions);
    }

    @Test
    public void test_that_wait_for_single_snapshot_to_become_available_returns_when_snapshot_status_is_available() {
        DBClusterSnapshot dbClusterSnapshot = new DBClusterSnapshot().withDBClusterSnapshotIdentifier("ssid").withStatus("available");

        when(rdsClientFactory.getClient(Regions.US_EAST_1)).thenReturn(amazonRDSClient);
        DescribeDBClusterSnapshotsResult result = new DescribeDBClusterSnapshotsResult().withDBClusterSnapshots(dbClusterSnapshot);
        when(amazonRDSClient.describeDBClusterSnapshots(any())).thenReturn(result);

        rdsService.waitForSnapshotsToBecomeAvailable(dbClusterSnapshot, Regions.US_EAST_1);
    }

    @Test
    public void test_that_generate_snapshot_identifier_returns_proper_result() {
        LocalDateTime.now();
        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(1555711439, 0, ZoneOffset.UTC);
        String snapshotId = rdsService.generateSnapshotIdentifier("test-cluster", localDateTime);
        assertEquals("test-cluster-2019-04-19-22-03", snapshotId);
    }
}
