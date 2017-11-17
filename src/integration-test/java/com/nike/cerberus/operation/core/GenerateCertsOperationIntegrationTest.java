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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.service.IdentityManagementService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static com.fieldju.commons.PropUtils.getPropWithDefaultValue;
import static com.fieldju.commons.PropUtils.getRequiredProperty;
import static com.nike.cerberus.service.CertificateService.DOMAIN_CERT_CHAIN_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_CERT_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_CSR_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_FULL_CERT_WITH_CHAIN_CRT_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_PKCS1_KEY_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_PKCS8_KEY_FILE;
import static com.nike.cerberus.service.CertificateService.DOMAIN_PUBLIC_KEY_FILE;
import static com.nike.cerberus.service.CertificateService.USER_KEY_FILE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This integration test will generate certs using an provided ACME api and test that the certs are usable my an AWS ALB and Netty's SSL Context Builder.
 *
 * Running this Integration Test will automatically accept the TOS of the ACME provider if present.
 * By Running this test you are agreeing with the Terms of Service of the ACME Provider API you provide.
 *
 * Required Props (can be set via env or system props)
 *
 * CERT_GEN_TEST_DOMAIN: A domain that will be used as the common name for the generated certs,
 *      you must also provide a hosted zone id that can create records for this domain.
 *
 * CERT_GEN_HOSTED_ZONE_ID: The hosted zone id that can create records for CERT_GEN_TEST_DOMAIN
 *
 * CERT_GEN_CONTACT_EMAIL: The email to use as the contact when generating the cert
 *
 * Optional Props
 *
 * CERT_GEN_ACME_API: the ACME api to test against, defaults to 'acme://letsencrypt.org/staging'
 *
 * EX: CERT_GEN_TEST_DOMAIN=cerberus-oss.io CERT_GEN_HOSTED_ZONE_ID=123123 CERT_GEN_CONTACT_EMAIL=justin.field@nike.com ./gradlew clean integrationTest -DintegrationTest.debug -DintegrationTest.single=GenerateCertsOperationIntegrationTest
 *
 */
public class GenerateCertsOperationIntegrationTest {

    public static String CERT_DIR = "build/certs/";

    @Mock
    private GenerateCertificateFilesCommand command;

    @Mock
    private ConsoleService consoleService;

    @Mock
    private EnvironmentMetadata environmentMetadata;

    @Mock
    private ConfigStore configStore;

    @Mock
    IdentityManagementService identityManagementService;

    private GenerateCertificateFilesOperation operation;

    private File certDir;

    @Before
    public void before() {
        initMocks(this);

        CertificateService certificateService = new CertificateService(consoleService,
                AmazonRoute53Client.builder().withRegion("us-west-2").build(), new UuidSupplier(), configStore,
                identityManagementService, environmentMetadata);

        operation = new GenerateCertificateFilesOperation(certificateService, environmentMetadata, consoleService);

        certDir = new File(CERT_DIR);
    }

    @Test
    public void test_that_command_generates_valid_certs() throws IOException {
        when(environmentMetadata.getName()).thenReturn("integration-test");
        when(environmentMetadata.getRegionName()).thenReturn("us-west-2");
        when(command.getBaseDomainName()).thenReturn(getRequiredProperty("CERT_GEN_TEST_DOMAIN",
                "CERT_GEN_TEST_DOMAIN is a required env or system prop and must be a domain that you have a" +
                        " hosted zone for that can create records"));
        when(command.getHostedZoneId()).thenReturn(getRequiredProperty("CERT_GEN_HOSTED_ZONE_ID",
                "The hosted zone id that can create records for CERT_GEN_TEST_DOMAIN"));
        when(consoleService.readLine(contains("Would you like to proceed?"))).thenReturn("y");
        when(command.enableLetsEncryptCertfix()).thenReturn(true);
        when(command.getCertDir()).thenReturn(certDir.getAbsolutePath());
        when(consoleService.readLine(contains("accept")))
                .thenReturn("i accept");
        when(command.getAcmeApiUrl()).thenReturn(getPropWithDefaultValue("CERT_GEN_ACME_API",
                "acme://letsencrypt.org/staging"));
        when(command.getContactEmail()).thenReturn(getRequiredProperty("CERT_GEN_CONTACT_EMAIL",
                "The email contact to use when generating the cert"));

        operation.run(command);

        ImmutableSet.of(
                USER_KEY_FILE,
                DOMAIN_PKCS1_KEY_FILE,
                DOMAIN_PKCS8_KEY_FILE,
                DOMAIN_CSR_FILE,
                DOMAIN_FULL_CERT_WITH_CHAIN_CRT_FILE,
                DOMAIN_CERT_FILE,
                DOMAIN_CERT_CHAIN_FILE,
                DOMAIN_PUBLIC_KEY_FILE
        ).forEach(expectedFileName -> {
            File expectedFile = new File(certDir.getAbsolutePath() + File.separator + expectedFileName);
            assertTrue("The expected file should exist after running the generate certs command", expectedFile.exists());
        });

        // test that the certs are usable by and ALB
        assertThatCertsAreUsableByAws();

        // test that the PKCS8 Private key and ACME generated cert are loadable by CMS
        assertThatCertsAreUsableByCms();

        System.out.println("The Keys and Certificates where properly generated and passed validation");
    }

    private void assertThatCertsAreUsableByAws() throws IOException {
        String uuid = UUID.randomUUID().toString();
        AmazonIdentityManagement amazonIdentityManagement = AmazonIdentityManagementClient.builder().withRegion(Regions.DEFAULT_REGION).build();
        final UploadServerCertificateRequest request = new UploadServerCertificateRequest()
                .withServerCertificateName("cert-gen-integration-test-" + uuid)
                .withPath("/cert-gen-integration-test/" + uuid + "/")
                .withCertificateBody(new String(Files.readAllBytes((new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CERT_FILE).toPath()))))
                .withCertificateChain(new String(Files.readAllBytes((new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CERT_CHAIN_FILE).toPath()))))
                .withPrivateKey(new String(Files.readAllBytes(new File(certDir.getAbsolutePath() + File.separator + DOMAIN_PKCS1_KEY_FILE).toPath())));

        final UploadServerCertificateResult result = amazonIdentityManagement.uploadServerCertificate(request);

        assertTrue(StringUtils.isNotBlank(result.getServerCertificateMetadata().getArn()));

        amazonIdentityManagement.deleteServerCertificate(
                new DeleteServerCertificateRequest()
                        .withServerCertificateName(result.getServerCertificateMetadata().getServerCertificateName())
        );
    }

    private void assertThatCertsAreUsableByCms() {
        String[] cmsSupportedProtocols = new String[] {
                "TLSv1.2"
        };

        try {
            SslContextBuilder.forServer(
                    new File(certDir.getAbsolutePath() + File.separator + DOMAIN_CERT_FILE),
                    new File(certDir.getAbsolutePath() + File.separator + DOMAIN_PKCS8_KEY_FILE)
            ).protocols(cmsSupportedProtocols).build();
        } catch (Exception e) {
            fail("Failed to generate the SSL Context that CMS uses to validate Cert and PKCS8 private key. Reason: " + e.getMessage());
        }
    }

}
