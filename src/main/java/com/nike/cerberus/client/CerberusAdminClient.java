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

package com.nike.cerberus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import com.nike.vault.client.http.HttpHeader;
import com.nike.vault.client.http.HttpMethod;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;

/**
 * A Cerberus admin client with the ability to restore metadata
 */
public class CerberusAdminClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    protected OkHttpClient httpClient;

    protected VaultCredentialsProvider credentialsProvider;

    /**
     * Explicit constructor that allows for full control over construction of the Vault client.
     *
     * @param credentialsProvider Credential provider for acquiring a token for interacting with Vault
     * @param httpClient          HTTP client for calling Vault
     */
    public CerberusAdminClient(VaultCredentialsProvider credentialsProvider,
                               OkHttpClient httpClient,
                               ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.credentialsProvider = credentialsProvider;
    }

    public void restoreMetadata(String baseUrl, String jsonPayload) {
        HttpUrl url = buildUrl(baseUrl, "v1/", "restore-sdb");
        Response response = execute(url, HttpMethod.PUT, jsonPayload);
        if (!response.isSuccessful()) {
            String body;
            try {
                body = response.body().string();
            } catch (IOException e) {
                body = e.getMessage();
            }
            throw new RuntimeException("Failed to restore metadata with cms body: " + response + '\n' + body);
        }
    }

    protected Response execute(final HttpUrl url, final String method, final String json) {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader(HttpHeader.VAULT_TOKEN, credentialsProvider.getCredentials().getToken())
                    .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString());

            requestBuilder.addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
                    .method(method, json != null ? RequestBody.create(DEFAULT_MEDIA_TYPE, json) : null);

            return httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            if (e instanceof SSLException // NOPMD
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
                throw new VaultClientException("I/O error while communicating with vault. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect", e);
            } else {
                throw new VaultClientException("I/O error while communicating with vault.", e);
            }
        }
    }

    /**
     * Builds the full URL for preforming an operation against Vault.
     *
     * @param baseUrl Base URL for Cerberus
     * @param prefix Prefix between the environment URL and specified path
     * @param path   Path for the requested operation
     * @return Full URL to execute a request against
     */
    protected HttpUrl buildUrl(String baseUrl, String prefix, String path) {
        if (!StringUtils.endsWith(baseUrl, "/")) {
            baseUrl += "/";
        }

        return HttpUrl.parse(baseUrl + prefix + path);
    }
}
