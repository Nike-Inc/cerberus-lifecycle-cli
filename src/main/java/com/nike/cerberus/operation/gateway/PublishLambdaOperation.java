package com.nike.cerberus.operation.gateway;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.google.common.io.Files;
import com.nike.cerberus.command.gateway.PublishLambdaCommand;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.operation.InvalidEnvironmentConfigException;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Publishes the lambda artifact to the Cerberus environment's configuration bucket.
 */
public class PublishLambdaOperation implements Operation<PublishLambdaCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final TransferManager transferManager;

    private final AmazonS3 amazonS3;

    @Inject
    public PublishLambdaOperation(final ConfigStore configStore, final AmazonS3 amazonS3) {
        this.configStore = configStore;
        this.transferManager = new TransferManager(amazonS3);
        this.amazonS3 = amazonS3;
    }

    @Override
    public void run(final PublishLambdaCommand command) {
        final URL artifactUrl = command.getArtifactUrl();

        final BaseOutputs outputParameters = configStore.getBaseStackOutputs();
        final String configBucketName = outputParameters.getConfigBucketName();

        if (StringUtils.isBlank(configBucketName)) {
            final String errorMessage = "The specified environment isn't configured properly!";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        initClient(configBucketName);
        final File filePath = downloadArtifact(artifactUrl);

        try {
            final Upload upload =
                    transferManager.upload(configBucketName, command.getLambdaName().getBucketKey(), filePath);
            logger.info("Uploading lambda artifact.");
            upload.waitForCompletion();
            logger.info("Uploading complete.");
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for upload to complete!", e);
        } finally {
            transferManager.shutdownNow(false);
        }
    }

    @Override
    public boolean isRunnable(final PublishLambdaCommand command) {
        try {
            this.configStore.getBaseStackOutputs();
            return true;
        } catch (IllegalStateException ise) {
            logger.error("The config bucket doesn't exist, yet! Aborting...");
            return false;
        }
    }

    private File downloadArtifact(final URL artifactUrl) {
        final File tempDir = Files.createTempDir();
        final String filePath = tempDir.getPath() + File.pathSeparator + "lambda.artifact";
        logger.debug("Downloading artifact to {}", tempDir.getAbsolutePath());

        try {
            ReadableByteChannel rbc = Channels.newChannel(artifactUrl.openStream());
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            logger.error("Failed to download the lambda artifact!", e);
            throw new IllegalStateException("Failed to download the lambda artifact.", e);
        }

        return new File(filePath);
    }

    private void initClient(final String configBucketName) {
        logger.debug("Initializing the S3 client");
        if (!amazonS3.doesBucketExist(configBucketName)) {
            final String errorMessage = String.format("The bucket specified doesn't exist! bucketName: %s",
                    configBucketName);
            logger.error(errorMessage);
            throw new InvalidEnvironmentConfigException(errorMessage);
        }
    }
}
