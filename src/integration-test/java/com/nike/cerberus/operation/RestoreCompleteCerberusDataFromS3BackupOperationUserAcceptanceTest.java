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
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.utils.TestUtils;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultAdminClient;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Optional;

import static com.nike.cerberus.operation.core.RestoreCompleteCerberusDataFromS3BackupOperation.DEFAULT_TIMEOUT;
import static com.nike.cerberus.operation.core.RestoreCompleteCerberusDataFromS3BackupOperation.DEFAULT_TIMEOUT_UNIT;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * A UAT test that can be run to manually verify the functionality of this
 * operation against any running Cerberus stack.
 */
public class RestoreCompleteCerberusDataFromS3BackupOperationUserAcceptanceTest {

    @Mock
    private VaultAdminClientFactory vaultAdminClientFactory;

    @Mock
    private ConsoleService consoleService;

    @Mock
    private RestoreCompleteCerberusDataFromS3BackupCommand command;

    private RestoreCompleteCerberusDataFromS3BackupOperation operation;

    private String rootToken;

    @Before
    public void before() {
        initMocks(this);

        operation = new RestoreCompleteCerberusDataFromS3BackupOperation(
                vaultAdminClientFactory,
                CerberusModule.configObjectMapper(),
                consoleService);

        when(command.getCerberusUrl()).thenReturn(TestUtils.getRequiredEnvVar("CERBERUS_URL",
                "The Cerberus API to restore against"));
        when(command.getS3Region()).thenReturn(TestUtils.getRequiredEnvVar("S3_REGION",
                "The region for the bucket that contains the backups"));
        when(command.getS3Bucket()).thenReturn(TestUtils.getRequiredEnvVar("S3_BUCKET",
                "The bucket that contains the backups"));
        when(command.getS3Prefix()).thenReturn(TestUtils.getRequiredEnvVar("S3_PREFIX",
                "the folder that contains the json backup files"));

        rootToken = TestUtils.getRequiredEnvVar("ROOT_TOKEN", "The root token for the destination Cerberus env");
    }

    @Test public void
    run_restore_complete() throws IOException {
        when(consoleService.readLine(anyString())).thenReturn("proceed");

        VaultAdminClient adminClient = new VaultAdminClient(
                new StaticVaultUrlResolver("http://127.0.0.1:8200"),
                new VaultAdminClientFactory.RootCredentialsProvider(rootToken),
                new OkHttpClient.Builder()
                        .hostnameVerifier(new NoopHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build()
        );

        Optional<VaultAdminClient> clientOptional = Optional.of(adminClient);

        when(vaultAdminClientFactory.getClientForLeader()).thenReturn(clientOptional);

        operation.run(command);
    }

}
