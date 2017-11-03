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

package com.nike.cerberus.command.core;

import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.command.core.RestoreCerberusBackupCommand;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.utils.TestUtils;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.StaticVaultUrlResolver;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;

import static com.nike.cerberus.client.CerberusAdminClientFactory.DEFAULT_TIMEOUT;
import static com.nike.cerberus.client.CerberusAdminClientFactory.DEFAULT_TIMEOUT_UNIT;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * A UAT test that can be run to manually verify the functionality of this
 * operation against any running Cerberus stack.
 */
public class RestoreCompleteCerberusDataFromS3BackupOperationUserAcceptanceTest {

    @Spy
    private CerberusAdminClientFactory vaultAdminClientFactory;

    @Mock
    private ConsoleService consoleService;

    private RestoreCerberusBackupCommand operation;

    private String rootToken;

    @Before
    public void before() {
        initMocks(this);

        operation = new RestoreCerberusBackupCommand(
                CerberusModule.configObjectMapper(),
                consoleService,
                vaultAdminClientFactory);

        operation.setCerberusUrl(TestUtils.getRequiredEnvVar("CERBERUS_URL", "The Cerberus API to restore against"));
        operation.setS3Region(TestUtils.getRequiredEnvVar("S3_REGION", "The region for the bucket that contains the backups"));
        operation.setS3Bucket(TestUtils.getRequiredEnvVar("S3_BUCKET", "The bucket that contains the backups"));
        operation.setS3Prefix(TestUtils.getRequiredEnvVar("S3_PREFIX", "the folder that contains the json backup files"));

        rootToken = TestUtils.getRequiredEnvVar("ROOT_TOKEN", "The root token for the destination Cerberus env");
    }

    @Test public void
    run_restore_complete() throws IOException {
        when(consoleService.readLine(anyString())).thenReturn("proceed");

        CerberusAdminClient adminClient = new CerberusAdminClient(
                new StaticVaultUrlResolver("http://127.0.0.1:8200"),
                new VaultAdminClientFactory.RootCredentialsProvider(rootToken),
                new OkHttpClient.Builder()
                        .hostnameVerifier(new NoopHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build(),
                CerberusModule.configObjectMapper()
        );

        when(vaultAdminClientFactory.createCerberusAdminClient(anyString())).thenReturn(adminClient);

        operation.execute();
    }

}