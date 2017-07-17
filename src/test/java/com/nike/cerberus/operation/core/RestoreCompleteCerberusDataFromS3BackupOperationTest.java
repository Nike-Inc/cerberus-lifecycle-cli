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

package com.nike.cerberus.operation.core;

import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.service.ConsoleService;
import com.nike.vault.client.VaultAdminClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

public class RestoreCompleteCerberusDataFromS3BackupOperationTest {

    private RestoreCerberusBackupOperation operation;

    @Mock
    private CerberusAdminClientFactory adminClientFactory;

    @Mock
    private ConsoleService consoleService;

    @Before
    public void before() {
        initMocks(this);
        operation = new RestoreCerberusBackupOperation(CerberusModule.configObjectMapper(), consoleService, adminClientFactory);
    }

    @Test
    public void test_that_process_backup_can_handle_nested_maps() throws IOException {
        String serializedDecryptedBackupJson = IOUtils.toString(
                getClass().getClassLoader()
                        .getResourceAsStream("com/nike/cerberus/operation/core/nested-map-sdb-backup.json")
        );

        CerberusAdminClient client = mock(CerberusAdminClient.class);
        RestoreCerberusBackupOperation operationSpy = spy(operation);
        doNothing().when(operationSpy).deleteAllSecrets(anyString(), any(VaultAdminClient.class));
        operationSpy.processBackup(serializedDecryptedBackupJson, client);


    }




}
