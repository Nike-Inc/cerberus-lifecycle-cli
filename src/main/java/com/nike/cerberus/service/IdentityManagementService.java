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

package com.nike.cerberus.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.CONFIG_REGION;

/**
 * Wrapper for AWS IAM.
 */
public class IdentityManagementService {

    private final AmazonIdentityManagement client;

    @Inject
    public IdentityManagementService(AwsClientFactory<AmazonIdentityManagementClient> identityManagementClientFactory,
                                     @Named(CONFIG_REGION) String configRegion) {

        // IAM is not region specific, the config region will suffice.
        client = identityManagementClientFactory.getClient(Regions.fromName(configRegion));
    }

    /**
     * Uploads a server certificate to AWS IAM.
     *
     * @param name  The server certificate name.  No spaces.
     * @param path  Path to store the certificate under.
     * @param body  PEM-encoded certificate body.
     * @param chain PEM-encoded certificate chain.
     * @param key   PEM-encoded certificate key.
     * @return The server certificate ID of the uploaded certificate.
     */
    public String uploadServerCertificate(String name,
                                          String path,
                                          String body,
                                          String chain,
                                          String key) {

        UploadServerCertificateRequest request = new UploadServerCertificateRequest()
                .withServerCertificateName(name)
                .withPath(sanitizePath(path))
                .withCertificateBody(body)
                .withCertificateChain(chain)
                .withPrivateKey(key);

        UploadServerCertificateResult result = client.uploadServerCertificate(request);

        return result.getServerCertificateMetadata().getServerCertificateId();
    }

    /**
     * Deletes the server certificate.
     *
     * @param name The server certificate name
     */
    public void deleteServerCertificate(String name) {
        client.deleteServerCertificate(new DeleteServerCertificateRequest().withServerCertificateName(name));
    }

    /**
     * Gets the ARN for the specified server certificate name.
     *
     * @param name The server certificate name
     * @return ARN
     */
    public Optional<String> getServerCertificateArn(String name) {
        try {
            GetServerCertificateResult serverCertificateResult =
                    client.getServerCertificate(new GetServerCertificateRequest().withServerCertificateName(name));
            return Optional.of(serverCertificateResult.getServerCertificate().getServerCertificateMetadata().getArn());
        } catch (NoSuchEntityException nsee) {
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
