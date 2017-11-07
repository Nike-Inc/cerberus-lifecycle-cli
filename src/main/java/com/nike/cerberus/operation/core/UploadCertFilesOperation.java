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

package com.nike.cerberus.operation.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import com.nike.cerberus.service.IdentityManagementService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Handles uploading of certificate files to IAM and the config store.
 */
public class UploadCertFilesOperation implements Operation<UploadCertFilesCommand> {

    private static final String CHAIN_FILE_NAME = CertificateService.CERT_CHAIN;
    private static final String CERT_FILE_NAME = CertificateService.DOMAIN_CERT;
    private static final String PKCS1_KEY_FILE_NAME = CertificateService.DOMAIN_PKCS1_KEY_FILE;
    private static final String PKCS8_KEY_FILE_NAME = CertificateService.DOMAIN_PKCS8_KEY_FILE;

    public static final Set<String> EXPECTED_FILE_NAMES = ImmutableSet.of(
            CHAIN_FILE_NAME,
            CERT_FILE_NAME,
            PKCS1_KEY_FILE_NAME,
            PKCS8_KEY_FILE_NAME
    );

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final IdentityManagementService identityManagementService;

    @Inject
    public UploadCertFilesOperation(final EnvironmentMetadata environmentMetadata,
                                    final UuidSupplier uuidSupplier,
                                    final ConfigStore configStore,
                                    final IdentityManagementService identityManagementService) {
        this.environmentMetadata = environmentMetadata;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.identityManagementService = identityManagementService;
    }

    @Override
    public void run(final UploadCertFilesCommand command) {
        final StackName stackName = command.getStackName();
        final Path certPath = command.getCertPath();
        final String caContents = getFileContents(certPath, CHAIN_FILE_NAME);
        final String certContents = getFileContents(certPath, CERT_FILE_NAME);
        final String keyContents = getFileContents(certPath, PKCS1_KEY_FILE_NAME);
        final String pkcs8KeyContents = getFileContents(certPath, PKCS8_KEY_FILE_NAME);
        final String certificateName = stackName.getName() + "_" + uuidSupplier.get();

        logger.info("Uploading certificate to IAM for {}, with name of {}.", stackName.getName(), certificateName);
        String id = identityManagementService.uploadServerCertificate(certificateName, getPath(),
                certContents, caContents, keyContents);

        logger.info("Cert ID: {}", id);

        logger.info("Uploading certificate parts to the configuration bucket.");
        configStore.storeCert(stackName, certificateName, caContents, certContents, keyContents, pkcs8KeyContents);

        logger.info("Uploading certificate completed.");
    }

    @Override
    public boolean isRunnable(final UploadCertFilesCommand command) {
        if (StringUtils.isBlank(environmentMetadata.getBucketName())) {
            logger.error("Environment isn't initialized, unable to upload certificate.");
            return false;
        }

        final String serverCertificateName = configStore.getServerCertificateName(command.getStackName());
        if (StringUtils.isNotBlank(serverCertificateName) && !command.isOverwrite()) {
            logger.error("Certificate already uploaded for this stack and environment.  Use --overwrite flag to force upload.");
            return false;
        }

        return true;
    }

    private String getFileContents(final Path path, final String filename) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(filename);

        File file = new File(path.toString(), filename);
        if (file.exists() && file.canRead()) {
            try {
                return new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read the following file: " + file.getAbsolutePath());
            }
        } else {
            throw new IllegalArgumentException("The file is not readable: " + file.getAbsolutePath());
        }
    }

    private String getPath() {
        return "/cerberus/" + environmentMetadata.getName() + "/";
    }
}
