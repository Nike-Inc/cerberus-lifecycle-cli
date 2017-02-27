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

package com.nike.cerberus.operation;

import com.nike.cerberus.command.core.RestoreCompleteCerberusDataFromS3BackupCommand;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.operation.core.RestoreCompleteCerberusDataFromS3BackupOperation;
import com.nike.cerberus.util.EnvVarUtils;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultAdminClient;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestoreCompleteCerberusDataFromS3BackupOperationIntegrationTest {

    private static final int DEFAULT_TIMEOUT = 15;

    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final String apiUrl = EnvVarUtils.getEnvVar("CERBERUS_URL", "Which Cerberus API to use");
    private final String token  = EnvVarUtils.getEnvVar("TOKEN", "The Token to use for restoring data");
    private final String bucket = EnvVarUtils.getEnvVar("S3_BUCKET", "The bucket that contains the backups");
    private final String prefix = EnvVarUtils.getEnvVar("S3_PREFIX", "The bucket that contains the backups");
    private final String s3Region = EnvVarUtils.getEnvVar("S3_BUCKET_REGION", "The Region that the backup bucket is in");

    private RestoreCompleteCerberusDataFromS3BackupOperation operation;

    @Before
    public void before() {
        VaultAdminClientFactory vaultAdminClientFactory = mock(VaultAdminClientFactory.class);
        VaultAdminClient adminClient = new VaultAdminClient(
                new StaticVaultUrlResolver("http://127.0.0.1:8200"),
                new VaultAdminClientFactory.RootCredentialsProvider(token),
                new OkHttpClient.Builder()
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build());
        when(vaultAdminClientFactory.getClientForLeader()).thenReturn(Optional.of(adminClient));

        CerberusModule module = new CerberusModule(null, null, null);
        operation = new RestoreCompleteCerberusDataFromS3BackupOperation(vaultAdminClientFactory, module.configObjectMapper());
    }

    @Test
    public void test_that_the_restore_complete_operation_can_restore_a_valid_backup_from_s3() {
        RestoreCompleteCerberusDataFromS3BackupCommand command = mock(RestoreCompleteCerberusDataFromS3BackupCommand.class);
        when(command.getS3Region()).thenReturn(s3Region);
        when(command.getS3Bucket()).thenReturn(bucket);
        when(command.getS3Prefix()).thenReturn(prefix);
        when(command.getCerberusUrl()).thenReturn(apiUrl);

        operation.run(command);


    }

}
