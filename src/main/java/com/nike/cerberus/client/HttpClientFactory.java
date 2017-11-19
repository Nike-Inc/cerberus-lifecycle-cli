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

package com.nike.cerberus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.auth.DefaultVaultCredentialsProviderChain;
import okhttp3.OkHttpClient;
import okio.Buffer;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.net.ssl.*;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HttpClientFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_TIMEOUT = 15;
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final ObjectMapper objectMapper;
    private final ConfigStore configStore;

    @Inject
    public HttpClientFactory(@Named(CerberusModule.CONFIG_OBJECT_MAPPER) ObjectMapper objectMapper,
                             ConfigStore configStore) {

        this.objectMapper = objectMapper;
        this.configStore = configStore;
    }

    /**
     * Admin client for doing admin cms tasks
     */
    public CerberusAdminClient createCerberusAdminClient(String url) {
        return new CerberusAdminClient(
                new StaticVaultUrlResolver(url),
                new DefaultVaultCredentialsProviderChain(),
                new OkHttpClient.Builder()
                        .hostnameVerifier(new NoopHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build(),
                objectMapper
        );
    }

    /**
     * @return Generic default client with timeouts for making manual http calls
     */
    public OkHttpClient getGenericClient() {
        return new OkHttpClient.Builder()
                .hostnameVerifier(new NoopHostnameVerifier())
                .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .build();
    }

    /**
     * Downloads the CA Chains from S3 for the Cerberus certs and creates a client that can talk to the
     * individual CMS instances without going through the alb, or manually having to add the chain to the trust store.
     */
    public OkHttpClient getGenericClientWithCustomTruststore() {
        try {
            List<String> caChains = new LinkedList<>();
            for (CertificateInformation certInfo : configStore.getCertificationInformationList()) {
                String certificateName = certInfo.getCertificateName();
                caChains.add(configStore.getCertPart(certificateName, ConfigConstants.CERT_PART_CA)
                        .orElseThrow(() -> new RuntimeException("Failed to download ca chain")));
            }

            Buffer buffer = new Buffer();
            caChains.forEach(buffer::writeUtf8);

            X509TrustManager trustManager = trustManagerForCertificates(buffer.inputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .hostnameVerifier(new NoopHostnameVerifier())
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                    .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                    .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to create ok http client with custom trust manager from ca certs downloaded from S3");
        }
    }

    /**
     * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/CustomTrust.java
     *
     * Returns a trust manager that trusts {@code certificates} and none other. HTTPS services whose
     * certificates have not been signed by these certificates will fail with a {@code
     * SSLHandshakeException}.
     *
     * <p>This can be used to replace the host platform's built-in trusted certificates with a custom
     * set. This is useful in development where certificate authority-trusted certificates aren't
     * available. Or in production, to avoid reliance on third-party certificate authorities.
     *
     *
     * <h3>Warning: Customizing Trusted Certificates is Dangerous!</h3>
     *
     * <p>Relying on your own trusted certificates limits your server team's ability to update their
     * TLS certificates. By installing a specific set of trusted certificates, you take on additional
     * operational complexity and limit your ability to migrate between certificate authorities. Do
     * not use custom trusted certificates in production without the blessing of your server's TLS
     * administrator.
     */
    private X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    /**
     * https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/CustomTrust.java
     */
    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}