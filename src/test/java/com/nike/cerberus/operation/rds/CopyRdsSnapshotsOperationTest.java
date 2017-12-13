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

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBClusterSnapshot;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.store.ConfigStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CopyRdsSnapshotsOperationTest {

    @Mock
    private ConfigStore configStore;

    private final static String envName = "test";

    @Mock
    private AwsClientFactory<AmazonRDSClient> clientFactory;

    private CopyRdsSnapshotsOperation operation;

    @Before
    public void before() {
        operation = new CopyRdsSnapshotsOperation(configStore, envName, clientFactory);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_true_if_id_matches() {
        boolean actual = operation.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:test-cerberus-database-cmsdatabasecluster-jns1lasdf9d-2017-12-12-18-07"));

        assertTrue(actual);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_false_for_a_different_env() {
        boolean actual = operation.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:dev-cerberus-database-cmsdatabasecluster-jns1lasdf9d-2017-12-12-18-07"));

        assertFalse(actual);
    }

    @Test
    public void test_that_wasSnapshotGeneratedFromCmsCluster_returns_false_if_id_does_not_matche() {
        boolean actual = operation.wasSnapshotGeneratedFromCmsCluster(new DBClusterSnapshot()
                .withDBClusterSnapshotIdentifier("rds:some-other-db-cluster-jns1lasdf9d-2017-12-12-18-07"));

        assertFalse(actual);
    }

    @Test
    public void test_that_isSnapshotNewerThan24Hours_returns_true_if_snapshot_is_newer_than_24h() {
        DBClusterSnapshot ss = new DBClusterSnapshot().withSnapshotCreateTime(Date.from(Instant.now().atZone(ZoneId.of("UTC")).toInstant()));

        boolean actual = operation.isSnapshotNewerThanGivenDays(ss, 1);

        assertTrue(actual);
    }

    @Test
    public void test_that_isSnapshotNewerThan24Hours_returns_false_if_snapshot_is_older_than_24h() {
        DBClusterSnapshot ss = new DBClusterSnapshot().withSnapshotCreateTime(Date.from(Instant.now().atZone(ZoneId.of("UTC"))
                .minus(1, ChronoUnit.WEEKS).toInstant()));

        boolean actual = operation.isSnapshotNewerThanGivenDays(ss, 1);

        assertFalse(actual);
    }

}
