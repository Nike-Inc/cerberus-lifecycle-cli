/*
 * Copyright (c) 2016 Nike Inc.
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

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Wrapper for AWS IAM.
 */
public class IdentityManagementService {

    private final AmazonIdentityManagement client;

    @Inject
    public IdentityManagementService(final AmazonIdentityManagement client) {
        this.client = client;
    }

    /**
     * Uploads a server certificate to AWS IAM.
     *
     * @param name The server certificate name.  No spaces.
     * @param path Path to store the certificate under.
     * @param body PEM-encoded certificate body.
     * @param chain PEM-encoded certificate chain.
     * @param key PEM-encoded certificate key.
     * @return The server certificate ID of the uploaded certificate.
     */
    public String uploadServerCertificate(
            final String name,
            final String path,
            final String body,
            final String chain,
            final String key) {
        final UploadServerCertificateRequest request = new UploadServerCertificateRequest()
                .withServerCertificateName(name)
                .withPath(sanitizePath(path))
                .withCertificateBody(body)
                .withCertificateChain(chain)
                .withPrivateKey(key);

        final UploadServerCertificateResult result = client.uploadServerCertificate(request);

        return result.getServerCertificateMetadata().getServerCertificateId();
    }

    /**
     * Deletes the server certificate.
     *
     * @param name The server certificate name
     */
    public void deleteServerCertificate(final String name) {
        client.deleteServerCertificate(new DeleteServerCertificateRequest().withServerCertificateName(name));
    }

    /**
     * Checks if the server certificate is present.
     *
     * @param name The server certificate name
     * @return If present
     */
    public boolean isServerCertificatePresent(final String name) {
        try {
            client.getServerCertificate(new GetServerCertificateRequest().withServerCertificateName(name));
            return true;
        } catch (final NoSuchEntityException nsee) {
            return false;
        }
    }

    /**
     * Gets the ARN for the specified server certificate name.
     *
     * @param name The server certificate name
     * @return ARN
     */
    public Optional<String> getServerCertificateArn(final String name) {
        try {
            final GetServerCertificateResult serverCertificateResult =
                    client.getServerCertificate(new GetServerCertificateRequest().withServerCertificateName(name));
            return Optional.of(serverCertificateResult.getServerCertificate().getServerCertificateMetadata().getArn());
        } catch (final NoSuchEntityException nsee) {
            return Optional.empty();
        }
    }

    /**
     * Gets the ID for the specified server certificate name.
     *
     * @param name The server certificate name
     * @return ID
     */
    public Optional<String> getServerCertificateId(final String name) {
        try {
            final GetServerCertificateResult serverCertificateResult =
                    client.getServerCertificate(new GetServerCertificateRequest().withServerCertificateName(name));
            return Optional.of(serverCertificateResult.getServerCertificate()
                    .getServerCertificateMetadata().getServerCertificateId());
        } catch (final NoSuchEntityException nsee) {
            return Optional.empty();
        }
    }

    private String sanitizePath(String path) {
        String p = path;
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (!p.endsWith("/")) {
            p += "/";
        }
        return p;
    }
}
