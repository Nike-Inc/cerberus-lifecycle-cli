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

package com.nike.cerberus.operation.dashboard;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.google.common.io.Files;
import com.nike.cerberus.command.dashboard.PublishDashboardCommand;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.operation.InvalidEnvironmentConfigException;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Publishes the dashboard artifact to the s3 bucket hosting the dashboard application.
 */
public class PublishDashboardOperation implements Operation<PublishDashboardCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final TransferManager transferManager;

    private final AmazonS3 amazonS3;

    @Inject
    public PublishDashboardOperation(final ConfigStore configStore, final AmazonS3 amazonS3) {
        this.configStore = configStore;
        this.transferManager = new TransferManager(amazonS3);
        this.amazonS3 = amazonS3;
    }

    @Override
    public void run(final PublishDashboardCommand command) {
        final URL artifactUrl = command.getArtifactUrl();
        final URL helpArtifactUrl = command.getHelpOverrideArtifactUrl();

        final BaseOutputs outputParameters = configStore.getBaseStackOutputs();
        final String dashboardBucketName = outputParameters.getDashboardBucketName();

        if (StringUtils.isBlank(dashboardBucketName)) {
            final String errorMessage = "The specified environment isn't configured properly!";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        initClient(dashboardBucketName);

        final File extractedDirectory = extractArtifact(artifactUrl, helpArtifactUrl);

        try {
            final MultipleFileUpload multipleFileUpload =
                    transferManager.uploadDirectory(dashboardBucketName, "", extractedDirectory, true, new DashboardMetaDataProvider());
            logger.info("Uploading dashboard files.");
            multipleFileUpload.waitForCompletion();
            logger.info("Uploading complete.");
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for upload to complete!", e);
        } finally {
            transferManager.shutdownNow(false);
        }
    }

    @Override
    public boolean isRunnable(final PublishDashboardCommand command) {
        try {
            this.configStore.getBaseStackOutputs();
            return true;
        } catch (IllegalStateException ise) {
            logger.error("The dashboard bucket doesn't exist, yet! Aborting...");
            return false;
        }
    }

    private File extractArtifact(final URL artifactUrl, final URL helpArtifactUrl) {
        final File extractionDirectory = Files.createTempDir();
        logger.info("Extracting artifact contents to {}", extractionDirectory.getAbsolutePath());

        downloadAndExtract(artifactUrl, extractionDirectory);

        if (helpArtifactUrl != null) {
            downloadAndExtract(helpArtifactUrl, extractionDirectory);
        }

        return extractionDirectory;
    }

    private void downloadAndExtract(URL artifactUrl, File extractionDirectory) {
        ArchiveEntry entry;
        TarArchiveInputStream tarArchiveInputStream = null;
        try {
            tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(artifactUrl.openStream()));
            entry = tarArchiveInputStream.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                if (entry.getName().startsWith("./")) {
                    entryName = entry.getName().substring(1);
                }
                final File destPath = new File(extractionDirectory, entryName);
                if (!entry.isDirectory()) {
                    final File fileParentDir = new File(destPath.getParent());
                    if (!fileParentDir.exists()) {
                        FileUtils.forceMkdir(fileParentDir);
                    }
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(destPath);
                        IOUtils.copy(tarArchiveInputStream, fileOutputStream);
                    } finally {
                        IOUtils.closeQuietly(fileOutputStream);
                    }
                }
                entry = tarArchiveInputStream.getNextEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream);
        }
    }

    private void initClient(final String dashboardBucketName) {
        logger.debug("Initializing the S3 client");
        if (!amazonS3.doesBucketExist(dashboardBucketName)) {
            final String errorMessage = String.format("The dashboard bucket specified doesn't exist! bucketName: %s", dashboardBucketName);
            logger.error(errorMessage);
            throw new InvalidEnvironmentConfigException(errorMessage);
        }
    }
}
