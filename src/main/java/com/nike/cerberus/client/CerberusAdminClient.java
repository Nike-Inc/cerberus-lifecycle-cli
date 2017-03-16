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

import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import com.nike.vault.client.http.HttpHeader;
import com.nike.vault.client.http.HttpMethod;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.SSLException;
import java.io.IOException;

/**
 * A Cerberus admin client with the ability to restore metadata
 */
public class CerberusAdminClient extends VaultAdminClient {

    protected OkHttpClient httpClient;
    protected VaultCredentialsProvider credentialsProvider;

    /**
     * Explicit constructor that allows for full control over construction of the Vault client.
     *
     * @param vaultUrlResolver    URL resolver for Vault
     * @param credentialsProvider Credential provider for acquiring a token for interacting with Vault
     * @param httpClient          HTTP client for calling Vault
     */
    public CerberusAdminClient(UrlResolver vaultUrlResolver,
                               VaultCredentialsProvider credentialsProvider,
                               OkHttpClient httpClient) {

        super(vaultUrlResolver, credentialsProvider, httpClient);
        this.httpClient = httpClient;
        this.credentialsProvider = credentialsProvider;
    }

    public void restoreMetadata(String jsonPayload) {
        HttpUrl url = buildUrl("v1/", "metadata");
        Response response = execute(url, HttpMethod.PUT, jsonPayload);
        if (! response.isSuccessful()) {
            throw new RuntimeException("Failed to restore metadata with cms body: " + response.message());
        }
    }

    protected Response execute(final HttpUrl url, final String method, final String json) {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader(HttpHeader.VAULT_TOKEN, credentialsProvider.getCredentials().getToken())
                    .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString());

            requestBuilder.addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
                    .method(method, RequestBody.create(DEFAULT_MEDIA_TYPE, json));

            return httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            if (e instanceof SSLException
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
                // AnyConnect web security proxy can be disabled with:
                //  `sudo /opt/cisco/anyconnect/bin/acwebsecagent -disablesvc -websecurity`
                throw new VaultClientException("I/O error while communicating with vault. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect", e);
            } else {
                throw new VaultClientException("I/O error while communicating with vault.", e);
            }
        }
    }
}
