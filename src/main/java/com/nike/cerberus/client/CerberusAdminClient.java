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
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nike.cerberus.domain.cms.SafeDepositBox;
import com.nike.cerberus.domain.cms.SdbMetadataResult;
import com.nike.vault.client.UrlResolver;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.auth.VaultCredentialsProvider;
import com.nike.vault.client.http.HttpHeader;
import com.nike.vault.client.http.HttpMethod;
import com.nike.vault.client.http.HttpStatus;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A Cerberus admin client with the ability to restore metadata
 */
public class CerberusAdminClient extends VaultAdminClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected OkHttpClient httpClient;
    protected VaultCredentialsProvider credentialsProvider;
    protected UrlResolver vaultUrlResolver;
    protected ObjectMapper objectMapper;

    protected final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .create();

    /**
     * Explicit constructor that allows for full control over construction of the Vault client.
     *
     * @param vaultUrlResolver    URL resolver for Vault
     * @param credentialsProvider Credential provider for acquiring a token for interacting with Vault
     * @param httpClient          HTTP client for calling Vault
     */
    public CerberusAdminClient(UrlResolver vaultUrlResolver,
                               VaultCredentialsProvider credentialsProvider,
                               OkHttpClient httpClient,
                               ObjectMapper objectMapper) {

        super(vaultUrlResolver, credentialsProvider, httpClient);
        this.httpClient = httpClient;
        this.credentialsProvider = credentialsProvider;
        this.vaultUrlResolver = vaultUrlResolver;
        this.objectMapper = objectMapper;
    }

    public void restoreMetadata(String jsonPayload) {
        HttpUrl url = buildUrl("v1/", "metadata");
        Response response = execute(url, HttpMethod.PUT, jsonPayload);
        if (! response.isSuccessful()) {
            String body;
            try {
                body = response.body().string();
            } catch (IOException e) {
                body = e.getMessage();
            }
            throw new RuntimeException("Failed to restore metadata with cms body: " + response + '\n' + body);
        }
    }

    public SdbMetadataResult getSDBMetaData(int offset, int limit) {
        URL baseUrl = null;
        try {
            baseUrl = new URL(vaultUrlResolver.resolve());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to process cerberus base url", e);
        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme(baseUrl.getProtocol())
                .host(baseUrl.getHost())
                .addPathSegments("v1/metadata")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", String.valueOf(offset))
                .build();

        Response response = execute(url, HttpMethod.GET, null);
        if (! response.isSuccessful()) {
            throw new RuntimeException(String.format("Failed to get metadata from Cerberus. Code: %s, Msg: %s",
                    response.code(), response.message()));
        }

        return parseCmsResponseBody(response, SdbMetadataResult.class);
    }

    public List<SafeDepositBox> getAllSdbMetadata() {
        List<SafeDepositBox> sdbMetadataList = new LinkedList<>();
        SdbMetadataResult currentResult = null;
        int offset = 0;
        int limit = 100;
        do {
            currentResult = getSDBMetaData(offset, limit);
            sdbMetadataList.addAll(currentResult.getSafeDepositBoxMetadata());
            offset = currentResult.getNextOffset();
            log.info("Retrieved metadata for {} SDBs", currentResult.getSdbCountInResult());
        } while (currentResult.hasNext());

        return sdbMetadataList;
    }

    public void writeJson(final String path, final Map<String, Object> data) {
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path);
        final Response response = execute(url, HttpMethod.POST, data);

        if (response.code() != HttpStatus.NO_CONTENT) {
            parseAndThrowErrorResponse(response);
        }
    }

    protected Response execute(final HttpUrl url, final String method, final String json) {
        try {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader(HttpHeader.VAULT_TOKEN, credentialsProvider.getCredentials().getToken())
                    .addHeader(HttpHeader.ACCEPT, DEFAULT_MEDIA_TYPE.toString());

            requestBuilder.addHeader(HttpHeader.CONTENT_TYPE, DEFAULT_MEDIA_TYPE.toString())
                    .method(method, json != null ? RequestBody.create(DEFAULT_MEDIA_TYPE, json) : null );

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

    /**
     * Convenience method for parsing the HTTP response and mapping it to a class.
     *
     * @param response      The HTTP response object
     * @param responseClass The class to map the response body to
     * @param <M>           Represents the type to map to
     * @return Deserialized object from the response body
     */
    protected <M> M parseCmsResponseBody(final Response response, final Class<M> responseClass) {
        try(ResponseBody body = response.body()) {
            return objectMapper.readValue(body.bytes(), responseClass);
        } catch (IOException e) {
            throw new VaultClientException("Error parsing the response body from CMS", e);
        }
    }

    /**
     * Read operation for a specified path.  Will return a {@link Map} of the data stored at the specified path.
     * If Vault returns an unexpected response code, a {@link VaultServerException} will be thrown with the code
     * and error details.  If an unexpected I/O error is encountered, a {@link VaultClientException} will be thrown
     * wrapping the underlying exception.
     *
     * @param path Path to the data
     * @return Map of the data
     */
    public GenericVaultResponse readDataGenerically(final String path) {
        final HttpUrl url = buildUrl(SECRET_PATH_PREFIX, path);
        log.debug("read: requestUrl={}", url);

        final Response response = execute(url, HttpMethod.GET, null);

        if (response.code() != HttpStatus.OK) {
            parseAndThrowErrorResponse(response);
        }

        return parseResponseBody(response, GenericVaultResponse.class);
    }

    public class GenericVaultResponse {
        private Map<String, Object> data;

        public Map<String, Object> getData() {
            return data;
        }

        public GenericVaultResponse setData(Map<String, Object> data) {
            this.data = data;
            return this;
        }
    }

    protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
        final String responseBodyStr = responseBodyAsString(response);
        try {
            return gson.fromJson(responseBodyStr, responseClass);
        } catch (JsonSyntaxException e) {
            log.error("parseResponseBody: responseCode={}, requestUrl={}",
                    response.code(), response.request().url());
            throw new VaultClientException("Error parsing the response body from vault, response code: " + response.code(), e);
        }
    }
}
