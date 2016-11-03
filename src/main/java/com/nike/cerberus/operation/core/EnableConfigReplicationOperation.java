package com.nike.cerberus.operation.core;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyResult;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketReplicationConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.ReplicationDestinationConfig;
import com.amazonaws.services.s3.model.ReplicationRule;
import com.amazonaws.services.s3.model.ReplicationRuleStatus;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.nike.cerberus.command.core.EnableConfigReplicationCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.domain.template.S3AssumeRoleInput;
import com.nike.cerberus.domain.template.S3ReplicationPolicyInput;
import com.nike.cerberus.generator.ConfigGenerationException;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Operation for enabling config replication.  This includes consul backups.
 */
public class EnableConfigReplicationOperation implements Operation<EnableConfigReplicationCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String replicationBucketNameTemplate = "%s-replicated-config-%s";

    private final String replicationRoleNameTemplate = "%s-cerberus-replication-role";

    private final String replicationPolicyNameTemplate = "%s-cerberus-replication-policy";

    private final String s3AssumeRoleTemplate = "templates/s3-assume-role.json.mustache";

    private final String s3ReplicationPolicyTemplate = "templates/s3-replication-policy.json.mustache";

    private final EnvironmentMetadata environmentMetadata;

    private final ConfigStore configStore;

    private final AmazonS3 s3Client;

    private final AmazonIdentityManagement iamClient;

    private final UuidSupplier uuidSupplier;

    private final MustacheFactory mustacheFactory;

    @Inject
    public EnableConfigReplicationOperation(final EnvironmentMetadata environmentMetadata,
                                            final ConfigStore configStore,
                                            final AmazonS3 s3Client,
                                            final AmazonIdentityManagement iamClient,
                                            final UuidSupplier uuidSupplier,
                                            final MustacheFactory mustacheFactory) {
        this.environmentMetadata = environmentMetadata;
        this.configStore = configStore;
        this.s3Client = s3Client;
        this.iamClient = iamClient;
        this.uuidSupplier = uuidSupplier;
        this.mustacheFactory = mustacheFactory;
    }

    @Override
    public void run(final EnableConfigReplicationCommand command) {
        final String replicationBucketName = createReplicationBucket(command);

        logger.info("Saving replication bucket name to config store.");
        configStore.storeReplicationBucketName(replicationBucketName);

        final String roleArn = createIamRoleForReplication(replicationBucketName);

        enabledReplication(roleArn, replicationBucketName);

        touchCurrentFiles();

        logger.info("Replication enabled!");
    }

    @Override
    public boolean isRunnable(final EnableConfigReplicationCommand command) {
        boolean isRunnable = true;
        final String baseStackId = configStore.getStackId(StackName.BASE);

        if (StringUtils.isBlank(baseStackId)) {
            isRunnable = false;
            logger.error("Base components must be initialized before creating the replication bucket!");
        }

        if (StringUtils.equalsIgnoreCase(environmentMetadata.getRegionName(), command.getReplicationRegion())) {
            isRunnable = false;
            logger.error("Replication region must be different than Cerberus hosted region!");
        }

        if (StringUtils.isNotBlank(configStore.getReplicationBucketName())) {
            isRunnable = false;
            logger.error("Replicaton bucket already exists, aborting...");
        }

        return isRunnable;
    }

    private String createReplicationBucket(final EnableConfigReplicationCommand command) {
        final Region originalRegion = s3Client.getRegion();
        try {
            s3Client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(command.getReplicationRegion())));

            // 1. Create the replication bucket.
            final String bucketName = String.format(replicationBucketNameTemplate,
                    environmentMetadata.getName(), uuidSupplier.get());

            final CreateBucketRequest createBucketRequest =
                    new CreateBucketRequest(bucketName);

            logger.info("Creating the replication bucket, {}", bucketName);
            s3Client.createBucket(createBucketRequest);

            // 2. Enable versioning on the replication bucket.
            final BucketVersioningConfiguration configuration =
                    new BucketVersioningConfiguration().withStatus("Enabled");
            final SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest =
                    new SetBucketVersioningConfigurationRequest(bucketName, configuration);

            logger.info("Enabling versioning on the replication bucket.");
            s3Client.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);

            return bucketName;
        } finally {
            s3Client.setRegion(originalRegion.toAWSRegion());
        }
    }

    private String createIamRoleForReplication(final String replicationBucketName) {
        final Mustache s3AssumeRoleTemplateCompiler = mustacheFactory.compile(s3AssumeRoleTemplate);
        final Mustache s3ReplicationPolicyTemplateCompiler = mustacheFactory.compile(s3ReplicationPolicyTemplate);
        final StringWriter s3AssumeRoleWriter = new StringWriter();
        final StringWriter s3ReplicationPolicyWriter = new StringWriter();
        final S3ReplicationPolicyInput s3ReplicationPolicyInput = new S3ReplicationPolicyInput();
        s3ReplicationPolicyInput.setSourceBucket(environmentMetadata.getBucketName());
        s3ReplicationPolicyInput.setReplicationBucket(replicationBucketName);

        try {
            s3AssumeRoleTemplateCompiler.execute(s3AssumeRoleWriter, new S3AssumeRoleInput()).flush();
            s3ReplicationPolicyTemplateCompiler.execute(s3ReplicationPolicyWriter, s3ReplicationPolicyInput).flush();
        } catch (IOException e) {
            throw new ConfigGenerationException("Failed to generate the policy documents for the replication role!", e);
        }

        // 1. Create the IAM role.
        final CreateRoleRequest createRoleRequest = new CreateRoleRequest();
        createRoleRequest.setRoleName(String.format(replicationRoleNameTemplate, environmentMetadata.getName()));
        createRoleRequest.setAssumeRolePolicyDocument(s3AssumeRoleWriter.toString());
        createRoleRequest.setPath("/");

        logger.info("Creating the IAM role for replication.");
        final CreateRoleResult createRoleResult = iamClient.createRole(createRoleRequest);

        // 2. Create the IAM policy.
        final CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest();
        createPolicyRequest.setPolicyName(String.format(replicationPolicyNameTemplate, environmentMetadata.getName()));
        createPolicyRequest.setPath("/");
        createPolicyRequest.setDescription("S3 bucket replication policy for Cerberus.");
        createPolicyRequest.setPolicyDocument(s3ReplicationPolicyWriter.toString());

        logger.info("Creating the IAM policy for replication.");
        final CreatePolicyResult createPolicyResult = iamClient.createPolicy(createPolicyRequest);

        // 3. Attach the policy to the role.
        final AttachRolePolicyRequest attachRolePolicyRequest = new AttachRolePolicyRequest();
        attachRolePolicyRequest.setRoleName(createRoleResult.getRole().getRoleName());
        attachRolePolicyRequest.setPolicyArn(createPolicyResult.getPolicy().getArn());

        logger.info("Attaching the policy to the IAM role.");
        iamClient.attachRolePolicy(attachRolePolicyRequest);

        return createRoleResult.getRole().getArn();
    }

    private void enabledReplication(final String roleArn, final String replicationBucketName) {
        final BucketReplicationConfiguration bucketReplicationConfiguration = new BucketReplicationConfiguration();
        bucketReplicationConfiguration.setRoleARN(roleArn);

        final ReplicationRule replicationRule = new ReplicationRule();
        replicationRule.setStatus(ReplicationRuleStatus.Enabled);
        replicationRule.setPrefix("");
        replicationRule.setDestinationConfig(
                new ReplicationDestinationConfig().withBucketARN("arn:aws:s3:::" + replicationBucketName));

        bucketReplicationConfiguration.addRule("replication-rule", replicationRule);

        logger.info("Enabling replication configuration on the config bucket.");
        s3Client.setBucketReplicationConfiguration(environmentMetadata.getBucketName(), bucketReplicationConfiguration);
    }

    private void touchCurrentFiles() {
        final String bucketName = environmentMetadata.getBucketName();
        final ObjectListing objectListing = s3Client.listObjects(bucketName);

        logger.info("Touching config files that already exist so they are replicated.");
        objectListing.getObjectSummaries().forEach(os -> {
            if (!StringUtils.startsWith(os.getKey(), "consul")) {
                logger.debug("Touching {}.", os.getKey());
                final S3Object object = s3Client.getObject(bucketName, os.getKey());
                s3Client.putObject(bucketName, object.getKey(), object.getObjectContent(), object.getObjectMetadata());
            }
        });
    }
}
