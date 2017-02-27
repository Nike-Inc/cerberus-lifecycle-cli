/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.vault;

import com.google.common.collect.Lists;
import com.nike.cerberus.domain.cloudformation.VaultOutputs;
import com.nike.cerberus.service.AutoScalingService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.auth.TokenVaultCredentials;
import com.nike.vault.client.auth.VaultCredentials;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import com.nike.vault.client.model.VaultHealthResponse;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import javax.inject.Inject;
import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Handles constructing a Vault admin client that can communicate with each Vault instance directly.
 */
public class VaultAdminClientFactory {

    private static final int DEFAULT_TIMEOUT = 15;

    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final Proxy proxy;

    private final AutoScalingService autoScalingService;

    private final ConfigStore configStore;

    @Inject
    public VaultAdminClientFactory(final Proxy proxy,
                                   final AutoScalingService autoScalingService,
                                   final ConfigStore configStore) {
        this.proxy = proxy;
        this.autoScalingService = autoScalingService;
        this.configStore = configStore;
    }

    /**
     * Looks up the running instances from the Vault AutoScaling group and attempts to determine who the leader is
     * by using the health endpoint.
     *
     * @return Client for leader
     */
    public Optional<VaultAdminClient> getClientForLeader() {
        VaultAdminClient leaderClient = null;
        final String vaultRootToken = configStore.getVaultRootToken();
        final VaultOutputs vaultOutputs = configStore.getVaultStackOutputs();
        final List<String> instanceDnsNames = autoScalingService.getPublicDnsForAutoScalingGroup(
                vaultOutputs.getAutoscalingGroupLogicalId());

        for (final String instanceDnsName : instanceDnsNames) {
            final VaultAdminClient vaultAdminClient = getClient(vaultRootToken, instanceDnsName);

            final VaultHealthResponse healthResponse = vaultAdminClient.health();

            if (healthResponse.isInitialized() && !healthResponse.isSealed() && !healthResponse.isStandby()) {
                leaderClient = vaultAdminClient;
                break;
            }
        }

        return Optional.ofNullable(leaderClient);
    }

    /**
     * Creates clients for each Vault instance in the AutoScaling group.
     *
     * @return List of Vault clients
     */
    public List<VaultAdminClient> getClientsForCluster() {
        final List<VaultAdminClient> clients = Lists.newLinkedList();
        String vaultRootToken = configStore.getVaultRootToken();

        final VaultOutputs vaultOutputs = configStore.getVaultStackOutputs();
        final List<String> instanceDnsNames = autoScalingService.getPublicDnsForAutoScalingGroup(
                vaultOutputs.getAutoscalingGroupLogicalId());
        instanceDnsNames.forEach(instanceDnsName -> {
            if (StringUtils.isNotBlank(instanceDnsName)) {
                clients.add(getClient(vaultRootToken, instanceDnsName));
            }
        });
        return clients;
    }

    /**
     * Determines if any Vault instances are present in the AutoScaling group.
     *
     * @return If instances running
     */
    public boolean hasVaultInstances() {
        final VaultOutputs vaultOutputs = configStore.getVaultStackOutputs();
        final List<String> instanceDnsNames = autoScalingService.getPublicDnsForAutoScalingGroup(
                vaultOutputs.getAutoscalingGroupLogicalId());
        return !instanceDnsNames.isEmpty();
    }

    public VaultAdminClient getClient(final String vaultRootToken, final String hostname) {
        final ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                .build();

        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .hostnameVerifier(new NoopHostnameVerifier())
                .connectionSpecs(Lists.newArrayList(spec))
                .proxy(proxy)
                .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                .build();

        return new VaultAdminClient(
                new StaticVaultUrlResolver(toVaultUrl(hostname)),
                new RootCredentialsProvider(vaultRootToken),
                httpClient);
    }

    private String toVaultUrl(final String hostname) {
        return String.format("https://%s:8200", hostname);
    }


    public static class RootCredentialsProvider implements VaultCredentialsProvider {

        private final String rootToken;

        public RootCredentialsProvider(final String rootToken) {
            this.rootToken = rootToken;
        }

        @Override
        public VaultCredentials getCredentials() {
            return new TokenVaultCredentials(rootToken);
        }
    }
}
