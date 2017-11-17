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
import com.nike.vault.client.StaticVaultUrlResolver;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class CerberusAdminClientFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_TIMEOUT = 60;
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final ObjectMapper objectMapper;

    @Inject
    public CerberusAdminClientFactory(@Named(CerberusModule.CONFIG_OBJECT_MAPPER) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CerberusAdminClient createCerberusAdminClient(String url) {
        return new CerberusAdminClient(
                new StaticVaultUrlResolver(url),
                null,
                new OkHttpClient.Builder()
                        .hostnameVerifier(new NoopHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build(),
                objectMapper
        );
    }

}
