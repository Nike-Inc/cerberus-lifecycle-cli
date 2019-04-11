/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Inject;
import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.command.core.RestoreCerberusBackupCommand;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.service.S3StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Operation for restoring Safe Deposit Box Metadata and Vault secret data for SDBs from backups that are in S3 from
 * the cross region backup lambda.
 */
public class RestoreCerberusBackupOperation implements Operation<RestoreCerberusBackupCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CERBERUS_BACKUP_METADATA_JSON_FILE_KEY = "cerberus-backup-metadata.json";

    private static final String CERBERUS_BACKUP_API_URL = "cerberusUrl";
    private static final String CERBERUS_BACKUP_DATE = "backupDate";
    private static final String CERBERUS_BACKUP_SDB_COUNT = "numberOfSdbs";

    private static final String CERBERUS_BACKUP_API_URL_S = "cerberus_url";
    private static final String CERBERUS_BACKUP_DATE_S = "backup_date";
    private static final String CERBERUS_BACKUP_SDB_COUNT_S = "number_of_sdbs";

    private final ObjectMapper objectMapper;
    private final ConsoleService console;
    private final CerberusAdminClientFactory cerberusAdminClientFactory;

    @Inject
    public RestoreCerberusBackupOperation(@Named(CerberusModule.CONFIG_OBJECT_MAPPER)
                                                  ObjectMapper objectMapper,
                                          ConsoleService console,
                                          CerberusAdminClientFactory cerberusAdminClientFactory) {

        this.objectMapper = objectMapper;
        this.console = console;
        this.cerberusAdminClientFactory = cerberusAdminClientFactory;
    }

    @Override
    public void run(RestoreCerberusBackupCommand command) {
        String backup = Chalk.on(String.format("s3://%s/%s",
                command.getS3Bucket(),
                command.getS3Prefix())
        ).yellow().bold().toString();

        logger.info(Chalk.on(
                String.format("Starting restore with backup located at %s", backup)
        ).green().toString());

        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(command.getS3Region()).build();
        S3StoreService s3StoreService = new S3StoreService(s3, command.getS3Bucket(), command.getS3Prefix());

        Set<String> keys = s3StoreService.getKeysInPartialPath("");
        if (keys.isEmpty()) {
            logger.error("There where no keys in {}/{}", command.getS3Bucket(), command.getS3Prefix());
        }

        if (!keys.contains(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY)) {
            throw new RuntimeException(
                    String.format("cerberus-backup-metadata.json was not found in s3://%s/%s/ is this a complete backup?",
                            command.getS3Bucket(), command.getS3Prefix()));
        }

        String kmsCustomerMasterKeyId = getKmsCmkId(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        S3StoreService s3EncryptionStoreService = getS3EncryptionStoreService(kmsCustomerMasterKeyId, command);
        CerberusAdminClient cerberusAdminClient = cerberusAdminClientFactory.createCerberusAdminClient();

        validateRestore(s3EncryptionStoreService, command);

        keys.remove(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY);
        Double i = 0d;
        DecimalFormat df = new DecimalFormat("#.##");
        for (String sdbBackupKey : keys) {
            try {
                Double percent = i / keys.size() * 100;
                System.out.print(String.format("Restoring backups %s%% complete\r", df.format(percent)));
                String sdbBackupJson = getDecryptedJson(sdbBackupKey, s3EncryptionStoreService);
                cerberusAdminClient.restoreMetadata(command.getCerberusUrl(), sdbBackupJson);
            } catch (Throwable t) {
                logger.error("Failed to process backup json for {}", Chalk.on(sdbBackupKey).red().toString(), t);
            }
            i++;
        }
        System.out.print("Restoring backups 100% complete\n");
        logger.info("Restore complete");
    }

    /**
     * Use the metadata from the backup and ensure that the user wants to proceed
     *
     * @param s3StoreService - The encrypted S3 store service
     */
    private void validateRestore(S3StoreService s3StoreService, RestoreCerberusBackupCommand command) {
        String backupMetadataJsonString = getDecryptedJson(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        Map<String, String> backupMetadata;
        try {
            backupMetadata = objectMapper.readValue(backupMetadataJsonString, new TypeReference<HashMap<String, String>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize backup metadata", e);
        }

        String backupApiUrl = backupMetadata.getOrDefault(CERBERUS_BACKUP_API_URL_S,
                backupMetadata.getOrDefault(CERBERUS_BACKUP_API_URL, "unknown"));
        String backupDate = backupMetadata.getOrDefault(CERBERUS_BACKUP_DATE_S,
                backupMetadata.getOrDefault(CERBERUS_BACKUP_DATE, "unknown"));
        String backupSdbCount = backupMetadata.getOrDefault(CERBERUS_BACKUP_SDB_COUNT_S,
                backupMetadata.getOrDefault(CERBERUS_BACKUP_SDB_COUNT, "unknown"));

        StringBuilder msg = new StringBuilder()
                .append("\nThe backup you are attempting to restore was created from ").append(Chalk.on(backupApiUrl).green().toString()).append(" on ").append(Chalk.on(backupDate).green().bold().toString())
                .append("\nThis backup contains ")
                .append(Chalk.on(backupSdbCount).green().toString())
                .append(" SDB records. ")
                .append("\nYou are attempting to restore this backup to ")
                .append(Chalk.on(command.getCerberusUrl()).green().toString());

        if (!backupApiUrl.equalsIgnoreCase(command.getCerberusUrl())) {
            msg.append("\n\n")
                    .append(Chalk.on("Warning: ").red().toString())
                    .append(Chalk.on("The backup was created for ").red().toString())
                    .append(Chalk.on(backupApiUrl).green().toString())
                    .append(Chalk.on("\nYou are attempting to restore to ").red().toString())
                    .append(Chalk.on(command.getCerberusUrl()).green().toString())
                    .append(Chalk.on("\nThese urls do not match, proceed with extreme caution!\n").red().toString());
        }

        msg.append("\nType \"proceed\" to restore this backup, anything else will exit");

        logger.info("");
        logger.info(Chalk.on("##########################################################################################").red().toString());
        logger.info(Chalk.on("#                                     DANGER ZONE                                        #").red().toString());
        logger.info(Chalk.on("##########################################################################################").red().toString());
        logger.info(msg.toString());

        String proceed;
        try {
            proceed = console.readLine("");
        } catch (IOException e) {
            throw new RuntimeException("Failed to validate that the user wanted to proceed with backup", e);
        }

        if (!proceed.equalsIgnoreCase("proceed")) {
            throw new RuntimeException("User did not confirm to proceed with backup restore");
        }
    }

    private S3StoreService getS3EncryptionStoreService(String cmkId,
                                                       RestoreCerberusBackupCommand command) {

        Region region = Region.getRegion(Regions.fromName(command.getS3Region()));
        KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(cmkId);
        AmazonS3EncryptionClient encryptionClient =
                new AmazonS3EncryptionClient(
                        new DefaultAWSCredentialsProviderChain(),
                        materialProvider,
                        new CryptoConfiguration()
                                .withAwsKmsRegion(region))
                        .withRegion(region);

        return new S3StoreService(encryptionClient, command.getS3Bucket(), command.getS3Prefix());
    }

    private String getKmsCmkId(String path, S3StoreService s3StoreService) {
        Map<String, String> metadata = s3StoreService.getS3ObjectUserMetaData(path);
        if (!metadata.containsKey("x-amz-matdesc")) {
            throw new RuntimeException("Failed to get Customer Master Key ID from object user metadata. " +
                    "'x-amz-matdesc' not found in metadata for object at path: " + path);
        }

        String serializedEncryptionContext = metadata.get("x-amz-matdesc");

        Map<String, String> encryptionContextMap;
        try {
            encryptionContextMap = objectMapper.readValue(serializedEncryptionContext,
                    new TypeReference<HashMap<String, String>>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert encryption context metadata value into Map");
        }
        return encryptionContextMap.get("kms_cmk_id");
    }

    private String getDecryptedJson(String sdbBackupKey, S3StoreService s3StoreService) {
        Optional<String> json = s3StoreService.get(sdbBackupKey);
        if (!json.isPresent()) {
            logger.error("Failed to get json from S3 for {}", sdbBackupKey);
        }
        return json.get();
    }

    @Override
    public boolean isRunnable(RestoreCerberusBackupCommand command) {
        return true;
    }

}
