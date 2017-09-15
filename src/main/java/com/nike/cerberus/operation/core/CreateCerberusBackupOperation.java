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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.command.core.SetBackupAdminPrincipalsCommand;
import com.nike.cerberus.domain.cms.SafeDepositBox;
import com.nike.cerberus.command.core.CreateCerberusBackupCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.backup.CerberusBackupMetadata;
import com.nike.cerberus.domain.backup.CerberusSdbMetadata;
import com.nike.cerberus.domain.environment.BackupRegionInfo;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.MetricsService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.store.ConfigStore;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.nike.cerberus.module.CerberusModule.getAWSCredentialsProviderChain;


public class CreateCerberusBackupOperation implements Operation<CreateCerberusBackupCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String AWS_PROVIDER = "AWS";
    private static final int MAX_BUCKET_NAME_LENGTH = 63;

    private final ObjectMapper objectMapper;
    private final ConfigStore configStore;
    private final CerberusAdminClient cerberusAdminClient;
    private final MetricsService metricsService;
    private final EnvironmentMetadata environmentMetadata;

    private final Map<String, S3StoreService> regionToEncryptedStoreServiceMap = new HashMap<>();

    @Inject
    public CreateCerberusBackupOperation(CerberusAdminClientFactory cerberusAdminClientFactory,
                                         ConfigStore configStore,
                                         MetricsService metricsService,
                                         EnvironmentMetadata environmentMetadata) {

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        objectMapper.setDateFormat(new ISO8601DateFormat());

        this.configStore = configStore;
        this.metricsService = metricsService;
        this.environmentMetadata = environmentMetadata;

        cerberusAdminClient = cerberusAdminClientFactory.createCerberusAdminClient(
                configStore.getCerberusBaseUrl());
    }

    @Override
    public void run(CreateCerberusBackupCommand command) {
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
            Map<String, Map<String, Object>> vaultData = recurseVault(sdb.getPath(), new HashMap<>());
            sdb.setData(vaultData);
            String key = sdb.getName().toLowerCase().replaceAll("\\W+", "-");
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

        regionsToStoreBackups.forEach(region -> {
            String bucket = configStore.getBackupInfoForRegion(region).get().getS3Bucket();
            log.info("Backup for region: {} complete, bucket: {}, prefix: {}", region, bucket, prefix);
        });
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
                "env", "cerberus-" + environmentMetadata.getName(),
                "region", environmentMetadata.getRegionName()
        );

        metricsService.trackGauge("numberOfSdbs", metadata.getNumberOfSdbs(), dimensions);
        metricsService.trackGauge("numberOfDataNodes", metadata.getNumberOfDataNodes(), dimensions);
        metricsService.trackGauge("numberOfKeyValuePairs", metadata.getNumberOfKeyValuePairs(), dimensions);
        metricsService.trackGauge("numberOfUniqueOwnerGroups", metadata.getNumberOfUniqueOwnerGroups(), dimensions);
        metricsService.trackGauge("numberOfUniqueIamRoles", metadata.getNumberOfUniqueIamRoles(), dimensions);
        metricsService.trackGauge("numberOfUniqueNonOwnerGroups", metadata.getNumberOfUniqueNonOwnerGroups(), dimensions);
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

        Map<String, Map<String, Object>> vaultNodes = sdb.getData();
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
    private Map<String, Map<String, Object>> recurseVault(String path, Map<String, Map<String, Object>> data) {
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
    private Map<String, Object> getData(String path) {
        CerberusAdminClient.GenericVaultResponse response = cerberusAdminClient.readDataGenerically(path);
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
            throw new RuntimeException(String.format("Failed to serialized SDB data, Prefix: %s, Key: %s", prefix, key), e);
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
        StringUtils.truncate(name, MAX_BUCKET_NAME_LENGTH);
        StringUtils.strip(name, "-");
        return name;
    }

    private String provisionKmsCmkForBackupRegion(String region) {
        Policy kmsPolicy = new Policy();
        final List<Statement> statements = new LinkedList<>();
        // allow the configured admin iam principals all permissions
        configStore.getBackupAdminIamPrincipals().forEach( principal -> {
            log.debug("Adding principal: {} to the CMK Policy for region {}", principal, region);
            statements.add(new Statement(Statement.Effect.Allow)
                .withId("Principal " + principal + " Has All Actions")
                .withPrincipals(new Principal(AWS_PROVIDER, principal, false))
                .withActions(KMSActions.AllKMSActions)
                .withResources(new Resource("*")));
        });

        kmsPolicy.setStatements(statements);

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
    public boolean isRunnable(CreateCerberusBackupCommand command) {
        if (configStore.getBackupAdminIamPrincipals().isEmpty()) {
            log.error("Backup Admin Principals have not been set please run " + SetBackupAdminPrincipalsCommand.COMMAND_NAME);
            return false;
        }
        return true;
    }

}
