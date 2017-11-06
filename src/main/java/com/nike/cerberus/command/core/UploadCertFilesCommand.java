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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.validator.UploadCertFilesPathValidator;
import com.nike.cerberus.command.validator.UploadCertFilesStackNameValidator;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.StackName;
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

import static com.nike.cerberus.command.core.UploadCertFilesCommand.COMMAND_NAME;

/**
 * Command for uploading a certificate for a specific component.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Upload certificate files for a specific component.")
public class UploadCertFilesCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "upload-cert";
    public static final String STACK_NAME_LONG_ARG = "--stack-name";
    public static final String CERT_PATH_LONG_ARG = "--cert-path";
    public static final String OVERWRITE_LONG_ARG = "--overwrite";

    private static final String CA_FILE_NAME = "ca.pem";
    private static final String CERT_FILE_NAME = "cert.pem";
    private static final String KEY_FILE_NAME = "key.pem";
    private static final String PKCS8_KEY_FILE_NAME = "pkcs8-key.pem";
    private static final String PUB_KEY_FILE_NAME = "pubkey.pem";
    public static final Set<String> EXPECTED_FILE_NAMES = ImmutableSet.of(CA_FILE_NAME, CERT_FILE_NAME, KEY_FILE_NAME, PKCS8_KEY_FILE_NAME, PUB_KEY_FILE_NAME);

    @Parameter(description = "Stack name the certificate is for.",
            names = { STACK_NAME_LONG_ARG },
            validateValueWith = UploadCertFilesStackNameValidator.class,
            required = true)
    private StackName stackName;

    @Parameter(description = "Path to the directory that contains the certificate files.",
            names = { CERT_PATH_LONG_ARG },
            validateValueWith = UploadCertFilesPathValidator.class,
            required = true)
    private Path certPath;

    @Parameter(description = "Overwrite the component certificate if it exists.", names = { OVERWRITE_LONG_ARG })
    private boolean overwrite;

    private final EnvironmentMetadata environmentMetadata;

    private final UuidSupplier uuidSupplier;

    private final ConfigStore configStore;

    private final IdentityManagementService identityManagementService;

    @Inject
    public UploadCertFilesCommand(final EnvironmentMetadata environmentMetadata,
                                    final UuidSupplier uuidSupplier,
                                    final ConfigStore configStore,
                                    final IdentityManagementService identityManagementService) {
        this.environmentMetadata = environmentMetadata;
        this.uuidSupplier = uuidSupplier;
        this.configStore = configStore;
        this.identityManagementService = identityManagementService;
    }

    @Override
    public void execute() {
        final String caContents = getFileContents(certPath, CA_FILE_NAME);
        final String certContents = getFileContents(certPath, CERT_FILE_NAME);
        final String keyContents = getFileContents(certPath, KEY_FILE_NAME);
        final String pkcs8KeyContents = getFileContents(certPath, PKCS8_KEY_FILE_NAME);
        final String pubKeyContents = getFileContents(certPath, PUB_KEY_FILE_NAME);
        final String certName = stackName.getName() + "_" + uuidSupplier.get();

        logger.info("Uploading certificate to IAM for {}, with name of {}.", stackName.getName(), certName);
        String id = identityManagementService.uploadServerCertificate(certName, getPath(), certContents, caContents, keyContents);
        logger.info("Cert ID: {}", id);

        logger.info("Uploading certificate parts to the configuration bucket.");
        configStore.storeCert(stackName, certName, caContents, certContents, keyContents, pkcs8KeyContents, pubKeyContents);

        logger.info("Uploading certificate completed.");
    }

    @Override
    public boolean isRunnable() {
        if (StringUtils.isBlank(environmentMetadata.getBucketName())) {
            logger.error("Environment isn't initialized, unable to upload certificate.");
            return false;
        }

        final String serverCertificateName = configStore.getServerCertificateName(stackName);
        if (StringUtils.isNotBlank(serverCertificateName) && ! overwrite) {
            logger.error("Certificate already uploaded for this stack and environment.  Use --overwrite flag to force upload.");
            return false;
        }

        return true;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
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
