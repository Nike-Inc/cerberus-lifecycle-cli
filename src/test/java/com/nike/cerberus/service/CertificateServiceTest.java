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

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CertificateServiceTest {

    @Mock private ConsoleService console;
    @Mock private AmazonRoute53Client route53;
    @Mock AwsClientFactory<AmazonRoute53Client> route53ClientFactory;
    @Mock private UuidSupplier uuidSupplier;
    @Mock private ConfigStore configStore;
    @Mock private IdentityManagementService identityManagementService;

    private CertificateService certificateService;

    @Before
    public void before() {
        initMocks(this);

        when(route53ClientFactory.getClient(any())).thenReturn(route53);
        certificateService = new CertificateService(
                console,
                route53ClientFactory,
                uuidSupplier,
                configStore,
                identityManagementService,
                "test",
                "us-west-2"
        );
    }

    /**
     * @throws IOException
     */
    @Test
    public void test_that_createPKCS8PrivateKeyPemFileFromKeyPair_creates_pkcs8_file() throws IOException {
        File certDir = new File("build/certs/" + UUID.randomUUID().toString());
        FileUtils.forceMkdir(certDir);
        KeyPair privateKey = certificateService.loadOrCreateKeyPair(
                new File(certDir.getAbsolutePath() + File.separator + CertificateService.DOMAIN_PKCS1_KEY_FILE));

        certificateService.createPKCS8PrivateKeyPemFileFromKeyPair(privateKey, certDir);

        File pkcs8 = new File(certDir.getAbsolutePath() + File.separator + CertificateService.DOMAIN_PKCS8_KEY_FILE);

        assertTrue(pkcs8.exists());
    }

}
