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
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.auth.DefaultVaultCredentialsProviderChain;

public class CerberusAdminClientFactory {

    private final ObjectMapper objectMapper;
    private final HttpClientFactory httpClientFactory;

    @Inject
    public CerberusAdminClientFactory(ObjectMapper objectMapper,
                                      HttpClientFactory httpClientFactory) {

        this.objectMapper = objectMapper;
        this.httpClientFactory = httpClientFactory;
    }


    /**
     * Admin client for doing admin cms tasks
     */
    public CerberusAdminClient createCerberusAdminClient(String url) {
        return new CerberusAdminClient(
                new StaticVaultUrlResolver(url),
                new DefaultVaultCredentialsProviderChain(),
                httpClientFactory.getGenericClient(),
                objectMapper
        );
    }

}
