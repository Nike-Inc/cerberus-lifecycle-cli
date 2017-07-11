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

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.cms.SafeDepositBox;
import com.nike.cerberus.command.core.CreateCompleteCerberusDataS3BackupCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.backup.CerberusBackupMetadata;
import com.nike.cerberus.domain.backup.CerberusSdbMetadata;
import com.nike.cerberus.domain.environment.BackupRegionInfo;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.MetricsService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.nike.cerberus.module.CerberusModule.getAWSCredentialsProviderChain;


public class CreateCompleteCerberusDataS3BackupOperation implements Operation<CreateCompleteCerberusDataS3BackupCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String AWS_PROVIDER = "AWS";

    private final ObjectMapper objectMapper;
    private final ConfigStore configStore;
    private final CerberusAdminClient cerberusAdminClient;
    private final MetricsService metricsService;
    private final EnvironmentMetadata environmentMetadata;
    private final AWSSecurityTokenService sts;
    private final Map<String, S3StoreService> regionToEncryptedStoreServiceMap = new HashMap<>();

    @Inject
    public CreateCompleteCerberusDataS3BackupOperation(CerberusAdminClientFactory cerberusAdminClientFactory,
                                                       @Named(CerberusModule.CONFIG_OBJECT_MAPPER)
                                                               ObjectMapper objectMapper,
                                                       ConfigStore configStore,
                                                       MetricsService metricsService,
                                                       EnvironmentMetadata environmentMetadata,
                                                       AWSSecurityTokenService sts) {

        this.objectMapper = objectMapper;
        this.configStore = configStore;
        this.metricsService = metricsService;
        this.environmentMetadata = environmentMetadata;
        this.sts = sts;

        cerberusAdminClient = cerberusAdminClientFactory.getNewCerberusAdminClient(
                configStore.getCerberusBaseUrl());
    }

    @Override
    public void run(CreateCompleteCerberusDataS3BackupCommand command) {
        log.info("Starting Cerberus Backup");

        List<String> regionsToStoreBackups = command.getBackupRegions();
        validateRegions(regionsToStoreBackups);

        Date now = new Date();
        String prefix = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now);
        List<SafeDepositBox> sdbMetadataList = cerberusAdminClient.getAllSdbMetadata();

        int sdbCount = sdbMetadataList.size();
        if (sdbCount < 1) {
            throw new RuntimeException("CMS returned no data when listing SDB metadata");
        } else {
            log.info(String.format("CMS returned that there are %s SDBs to back up.", sdbMetadataList.size()));
        }

        CerberusSdbMetadata cerberusSdbMetadata = new CerberusSdbMetadata();
        for (SafeDepositBox sdb : sdbMetadataList) {
            log.info(String.format("Backing up %s", sdb.getName()));
            Map<String, Map<String, String>> vaultData = recurseVault(sdb.getPath(), new HashMap<>());
            sdb.setData(vaultData);
            String key = sdb.getName().toLowerCase().replaceAll("/\\W+/", "-");
            saveDataToS3(sdb, prefix, key, regionsToStoreBackups);
            cerberusSdbMetadata = processMetadata(sdb, cerberusSdbMetadata);
        }

        // save metadata
        CerberusBackupMetadata metadata = new CerberusBackupMetadata()
                .setCerberusUrl(configStore.getCerberusBaseUrl())
                .setBackupDate(now)
                .setNumberOfSdbs(sdbMetadataList.size())
                .setNumberOfDataNodes(cerberusSdbMetadata.getNumberOfDataNodes())
                .setNumberOfKeyValuePairs(cerberusSdbMetadata.getNumberOfKeyValuePairs())
                .setNumberOfUniqueOwnerGroups(cerberusSdbMetadata.getUniqueOwnerGroups().size())
                .setNumberOfUniqueIamRoles(cerberusSdbMetadata.getUniqueIamRoles().size())
                .setNumberOfUniqueNonOwnerGroups(cerberusSdbMetadata.getUniqueNonOwnerGroups().size());

        if (metadata.getNumberOfKeyValuePairs() < 1) {
            throw new RuntimeException("The number of backed up key value pairs was less than 1, this probably means something bad is going on");
        }

        String key = "cerberus-backup-metadata.json";

        saveDataToS3(metadata, prefix, key, regionsToStoreBackups);

        trackMetadataMetrics(metadata);
    }

    /**
     * Vaildates that the user inputed regions are valid AWS regions
     *
     * @param regionsToStoreBackups The user inputed list of regions
     */
    private void validateRegions(List<String> regionsToStoreBackups) {
        regionsToStoreBackups.forEach(region -> {
            try {
                Regions.fromName(region);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(String.format("The region %s is not a valid region", region));
            }
        });
    }

    private void trackMetadataMetrics(CerberusBackupMetadata metadata) {
        Map<String, String> dimensions = ImmutableMap.of(
                "cerberusUrl", metadata.getCerberusUrl(),
                "env", environmentMetadata.getName(),
                "region", environmentMetadata.getRegionName()
        );

        metricsService.trackGauge("numberOfSdbs", metadata.getNumberOfSdbs(), dimensions);
        metricsService.trackGauge("numberOfDataNodes", metadata.getNumberOfDataNodes(), dimensions);
        metricsService.trackGauge("numberOfKeyValuePairs", metadata.getNumberOfKeyValuePairs(), dimensions);
        metricsService.trackGauge("numberOfUniqueOwnerGroups", metadata.getNumberOfUniqueIamRoles(), dimensions);
        metricsService.trackGauge("numberOfUniqueIamRoles", metadata.getNumberOfUniqueIamRoles(), dimensions);
        metricsService.trackGauge("numberOfUniqueNonOwnerGroups", metadata.getNumberOfUniqueIamRoles(), dimensions);
    }

    /**
     * Method to keep track of metadata
     */
    private CerberusSdbMetadata processMetadata(SafeDepositBox sdb, final CerberusSdbMetadata currentMetadata) {
        CerberusSdbMetadata newMetadata = new CerberusSdbMetadata(
                currentMetadata.getNumberOfKeyValuePairs(),
                currentMetadata.getNumberOfDataNodes(),
                currentMetadata.getUniqueOwnerGroups(),
                currentMetadata.getUniqueIamRoles(),
                currentMetadata.getUniqueNonOwnerGroups()
        );

        newMetadata.getUniqueIamRoles().add(sdb.getOwner());
        sdb.getIamRolePermissions().forEach((iamRole, permission) -> {
            newMetadata.getUniqueIamRoles().add(iamRole);
        });
        sdb.getUserGroupPermissions().forEach((userGroup, permission) -> {
            newMetadata.getUniqueNonOwnerGroups().add(userGroup);
        });

        Map<String, Map<String, String>> vaultNodes = sdb.getData();
        newMetadata.setNumberOfDataNodes(newMetadata.getNumberOfDataNodes() + vaultNodes.size());
        vaultNodes.forEach((path, kvPairs) -> {
            newMetadata.setNumberOfKeyValuePairs(newMetadata.getNumberOfKeyValuePairs() + kvPairs.size());
        });
        return newMetadata;
    }

    /**
     * Recurse a Vault path for data.
     *
     * @param path The path to recurse
     * @return Map of Vault path Strings to Maps of String, String containing the secret kv pairs
     */
    private Map<String, Map<String, String>> recurseVault(String path, Map<String, Map<String, String>> data) {
        List<String> keys = getKeys(path);

        keys.forEach(key -> {
            String compositeKey = path + key;
            if (key.endsWith("/")) {
                recurseVault( compositeKey, data);
            } else {
                data.put(compositeKey, getData(compositeKey));
            }
        });

        return data;
    }

    /**
     * Lists keys for a vault path.
     *
     * @param path The path in Vault to list keys for
     * @return List of keys, sub folders with have trailing /
     */
    private List<String> getKeys(String path) {
        VaultListResponse vaultListResponse = cerberusAdminClient.list(path);
        return vaultListResponse.getKeys();
    }

    /**
     * Downloads Vault data for a given path.
     *
     * @param path The path of data to download
     * @return The data map
     */
    private Map<String, String> getData(String path) {
        VaultResponse response = cerberusAdminClient.read(path);
        return response.getData();
    }

    /**
     * Using an S3 Encryption client saves the sdb data to the backup bucket / kms key
     * @param object The sdb data to back up
     * @param prefix The prefix / virtual folder to store the encrypted json
     */
    private void saveDataToS3(Object object, String prefix, String key, List<String> regions) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialized SDB data");
        }

        for (String region : regions) {
            S3StoreService storeService = regionToEncryptedStoreServiceMap.getOrDefault(region,
                    getEncryptedStoreServiceForRegion(region));

            storeService.put(prefix + '/' + key, json);
        }
    }

    private S3StoreService getEncryptedStoreServiceForRegion(String region) {
        Optional<BackupRegionInfo> backupRegionInfo = configStore.getBackupInfoForRegion(region);

        if (! backupRegionInfo.isPresent()) {
            String kmsCmkId = provisionKmsCmkForBackupRegion(region);
            String backupBucket = provisionBackupBucketForRegion(region);
            configStore.storeBackupInfoForRegion(region, backupBucket, kmsCmkId);
            backupRegionInfo = Optional.of(new BackupRegionInfo(backupBucket, kmsCmkId));
        }

        KMSEncryptionMaterialsProvider materialProvider =
                new KMSEncryptionMaterialsProvider(backupRegionInfo.get().getKmsCmkId());

        AmazonS3Encryption encryptionClient =
                AmazonS3EncryptionClientBuilder.standard()
                        .withCredentials(getAWSCredentialsProviderChain())
                        .withEncryptionMaterials(materialProvider)
                        .withCryptoConfiguration(new CryptoConfiguration()
                                .withAwsKmsRegion(Region.getRegion(Regions.fromName(region))))
                        .withRegion(region)
                        .build();

        S3StoreService storeService = new S3StoreService(encryptionClient, backupRegionInfo.get().getS3Bucket(), "");
        regionToEncryptedStoreServiceMap.put(region, storeService);
        return storeService;
    }

    private String provisionBackupBucketForRegion(String region) {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(getAWSCredentialsProviderChain())
                .withRegion(region)
                .build();

        Bucket bucket = s3.createBucket(getBackupBucketName(region));
        String bucketName = bucket.getName();
        log.info("Created bucket in region: {} with name: {}", region, bucketName);
        return bucketName;
    }

    private String getBackupBucketName(String region) {
        String name = String.format("%s-%s-backup-%s", environmentMetadata.getName(), region, UUID.randomUUID().toString());
        if (name.length() > 63) {
            name = name.substring(0, 62);
        }
        if (name.endsWith("-")) {
            name = name.substring(0, name.length() - 2);
        }
        return name;
    }

    private String provisionKmsCmkForBackupRegion(String region) {
        GetCallerIdentityResult identityResult = sts.getCallerIdentity(new GetCallerIdentityRequest());
        String accountId = identityResult.getAccount();
        String allRolesInAccount = String.format("arn:aws:iam::%s:role/*", accountId);
        String rootArn = String.format("arn:aws:iam::%s:root", accountId);

        String adminRoleArn = configStore.getAccountAdminArn().get();

        Policy kmsPolicy = new Policy();

        // allow the root user all permissions
        Statement rootUserStatement = new Statement(Statement.Effect.Allow);
        rootUserStatement.withId("Root User Has All Actions");
        rootUserStatement.withPrincipals(new Principal(AWS_PROVIDER, rootArn, false));
        rootUserStatement.withActions(KMSActions.AllKMSActions);
        rootUserStatement.withResources(new Resource("*"));

        // allow the configured admin user all permissions
        Statement adminUserStatement = new Statement(Statement.Effect.Allow);
        adminUserStatement.withId("Admin Role Has All Actions");
        adminUserStatement.withPrincipals(new Principal(AWS_PROVIDER, adminRoleArn, false));
        adminUserStatement.withActions(KMSActions.AllKMSActions);
        adminUserStatement.withResources(new Resource("*"));

        kmsPolicy.withStatements(
            rootUserStatement,
            adminUserStatement
        );

        String policyString = kmsPolicy.toJson();

        log.debug("Creating key for region {} with policy {}", region, policyString);

        AWSKMS kms = AWSKMSClient.builder().withCredentials(getAWSCredentialsProviderChain()).withRegion(region).build();
        CreateKeyResult createKeyResult = kms.createKey(
                new CreateKeyRequest()
                    .withPolicy(policyString)
                    .withBypassPolicyLockoutSafetyCheck(true)
                    .withDescription(String.format("Cerberus Backup Encryption key for env: %S region: %s",
                            environmentMetadata.getName(), region))
                    .withTags(
                            new Tag().withTagKey("env").withTagValue(environmentMetadata.getName()),
                            new Tag().withTagKey("region").withTagValue(region),
                            new Tag().withTagKey("cerberus-backup-key").withTagValue("true")

                    )
        );

        String keyId = createKeyResult.getKeyMetadata().getKeyId();

        log.info("Created new backup KMS CMK with id: {} for region: {}", keyId, region);

        return keyId;
    }

    @Override
    public boolean isRunnable(CreateCompleteCerberusDataS3BackupCommand command) {
        Optional<String> adminIamPrincipalArn = configStore.getAccountAdminArn();
        if (! adminIamPrincipalArn.isPresent()) {
            log.error("The admin IAM principal must be set for this environment");
            return false;
        }
        return true;
    }

}
