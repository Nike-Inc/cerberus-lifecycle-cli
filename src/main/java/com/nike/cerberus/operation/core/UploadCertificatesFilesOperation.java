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

package com.nike.cerberus.operation.core;

import com.nike.cerberus.command.core.UploadCertificateFilesCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CertificateService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Handles uploading of certificate files to IAM and the config store.
 */
public class UploadCertificatesFilesOperation implements Operation<UploadCertificateFilesCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;
    private final CertificateService certificateService;

    @Inject
    public UploadCertificatesFilesOperation(EnvironmentMetadata environmentMetadata,
                                            CertificateService certificateService) {

        this.environmentMetadata = environmentMetadata;
        this.certificateService = certificateService;
    }

    @Override
    public void run(final UploadCertificateFilesCommand command) {
        certificateService.uploadCertFiles(command.getCertPath().toFile());
    }

    @Override
    public boolean isRunnable(final UploadCertificateFilesCommand command) {
        if (StringUtils.isBlank(environmentMetadata.getBucketName())) {
            logger.error("Environment isn't initialized, unable to upload certificate.");
            return false;
        }

        return true;
    }
}
