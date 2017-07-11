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
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultTokenAuthRequest;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CerberusAdminClientFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_TIMEOUT = 60;
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final VaultAdminClientFactory vaultAdminClientFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public CerberusAdminClientFactory(VaultAdminClientFactory vaultAdminClientFactory,
                                      @Named(CerberusModule.CONFIG_OBJECT_MAPPER) ObjectMapper objectMapper) {

        this.vaultAdminClientFactory = vaultAdminClientFactory;
        this.objectMapper = objectMapper;
    }

    public CerberusAdminClient getNewCerberusAdminClient(String url) {
        return new CerberusAdminClient(
                new StaticVaultUrlResolver(url),
                new VaultAdminClientFactory.RootCredentialsProvider(generateAdminToken()),
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
     * Generates and admin token that CMS will recognize as an Admin so we can us the restore endpoint
     */
    private String generateAdminToken() {
        VaultAuthResponse vaultAuthResponse;
        try {
            logger.info("Attempting to generate an admin token with the root token");
            VaultAdminClient adminClient = vaultAdminClientFactory.getClientForLeader().get();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("is_admin", "true");
            metadata.put("username", "admin-cli");

            Set<String> policies = new HashSet<>();
            policies.add("root");

            vaultAuthResponse = adminClient.createOrphanToken(new VaultTokenAuthRequest()
                    .setDisplayName("admin-cli")
                    .setPolicies(policies)
                    .setMeta(metadata)
                    .setTtl("30m")
                    .setNoDefaultPolicy(true));
        } catch (VaultClientException e) {
            throw new RuntimeException("There was an error while trying to create an admin token, this command " +
                    "requires proxy access or direct a connect to the vault leader, is your ip white listed?", e);
        }

        return vaultAuthResponse.getClientToken();
    }
}
