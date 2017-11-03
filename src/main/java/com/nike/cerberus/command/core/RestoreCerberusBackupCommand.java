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

package com.nike.cerberus.command.core;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Inject;
import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.client.CerberusAdminClientFactory;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.model.VaultListResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

/**
 * Command for restoring Safe Deposit Box Metadata and Vault secret data for SDBs from backups that are in S3 from
 * the cross region backup lambda.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Allows Cerberus operators to restore a complete backup from S3 that was created using the the backup command."
)
public class RestoreCerberusBackupCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "restore-complete";

    private static final String CERBERUS_BACKUP_METADATA_JSON_FILE_KEY = "cerberus-backup-metadata.json";

    private static final String CERBERUS_BACKUP_API_URL = "cerberusUrl";
    private static final String CERBERUS_BACKUP_DATE = "backupDate";
    private static final String CERBERUS_BACKUP_SDB_COUNT = "numberOfSdbs";

    private static final String CERBERUS_BACKUP_API_URL_S = "cerberus_url";
    private static final String CERBERUS_BACKUP_DATE_S = "backup_date";
    private static final String CERBERUS_BACKUP_SDB_COUNT_S = "number_of_sdbs";

    @Parameter(names = "-s3-region", description = "The region for the bucket that contains the backups", required = true)
    private String s3Region;

    @Parameter(names = "-s3-bucket", description = "The bucket that contains the backups", required = true)
    private String s3Bucket;

    @Parameter(names = "-s3-prefix", description = "the folder that contains the json backup files", required = true)
    private String s3Prefix;

    @Parameter(names = "-url", description = "The cerberus api, to restore to", required = true)
    private String cerberusUrl;

    private final ObjectMapper objectMapper;
    private final ConsoleService console;
    private final CerberusAdminClientFactory cerberusAdminClientFactory;

    @Inject
    public RestoreCerberusBackupCommand(@Named(CerberusModule.CONFIG_OBJECT_MAPPER) ObjectMapper objectMapper,
                                          ConsoleService console,
                                          CerberusAdminClientFactory cerberusAdminClientFactory) {

        this.objectMapper = objectMapper;
        this.console = console;
        this.cerberusAdminClientFactory = cerberusAdminClientFactory;
    }

    @Override
    public void execute() {
        String backup = Chalk.on(String.format("s3://%s/%s",s3Bucket, s3Prefix)).yellow().bold().toString();

        logger.info(Chalk.on(
                String.format("Starting restore with backup located at %s", backup)
        ).green().toString());

        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(s3Region).build();
        S3StoreService s3StoreService = new S3StoreService(s3, s3Bucket, s3Prefix);

        Set<String> keys = s3StoreService.getKeysInPartialPath("");
        if (keys.isEmpty()) {
            logger.error("There where no keys in {}/{}", s3Bucket, s3Prefix);
        }

        if (! keys.contains(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY)) {
            throw new RuntimeException(
                    String.format("cerberus-backup-metadata.json was not found in s3://%s/%s/ is this a complete backup?",
                            s3Bucket, s3Prefix));
        }

        String kmsCustomerMasterKeyId = getKmsCmkId(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        S3StoreService s3EncryptionStoreService = getS3EncryptionStoreService(kmsCustomerMasterKeyId);
        CerberusAdminClient cerberusAdminClient = cerberusAdminClientFactory.createCerberusAdminClient(cerberusUrl);

        validateRestore(s3EncryptionStoreService);

        keys.remove(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY);
        Double i = 0d;
        DecimalFormat df = new DecimalFormat("#.##");
        for (String sdbBackupKey : keys) {
            try {
                Double percent = i / keys.size() * 100;
                System.out.print(String.format("Restoring backups %s%% complete\r", df.format(percent)));
                String json = getDecryptedJson(sdbBackupKey, s3EncryptionStoreService);
                processBackup(json, cerberusAdminClient);
            } catch (Throwable t) {
                logger.error("Failed to process backup json for {}", Chalk.on(sdbBackupKey).red().toString(), t);
            }
            i++;
        }
        System.out.print("Restoring backups 100% complete\n");
        logger.info("Restore complete");
    }

    @Override
    public boolean isRunnable() {
        return true;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    /**
     * Use the metadata from the backup and ensure that the user wants to proceed
     * @param s3StoreService - The encrypted S3 store service
     */
    private void validateRestore(S3StoreService s3StoreService) {
        String backupMetadataJsonString = getDecryptedJson(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        Map<String, String> backupMetadata;
        try {
            backupMetadata = objectMapper.readValue(backupMetadataJsonString, new TypeReference<HashMap<String,String>>() {});
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
                .append(Chalk.on(cerberusUrl).green().toString());

        if (! backupApiUrl.equalsIgnoreCase(cerberusUrl)) {
            msg.append("\n\n")
                    .append(Chalk.on("Warning: ").red().toString())
                    .append(Chalk.on("The backup was created for ").red().toString())
                    .append(Chalk.on(backupApiUrl).green().toString())
                    .append(Chalk.on("\nYou are attempting to restore to ").red().toString())
                    .append(Chalk.on(cerberusUrl).green().toString())
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

        if (! proceed.equalsIgnoreCase("proceed")) {
            throw new RuntimeException("User did not confirm to proceed with backup restore");
        }
    }

    private S3StoreService getS3EncryptionStoreService(String cmkId) {

        Region region = Region.getRegion(Regions.fromName(s3Region));
        KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(cmkId);
        AmazonS3EncryptionClient encryptionClient =
                new AmazonS3EncryptionClient(
                        new DefaultAWSCredentialsProviderChain(),
                        materialProvider,
                        new CryptoConfiguration()
                                .withAwsKmsRegion(region))
                        .withRegion(region);

        return new S3StoreService(encryptionClient, s3Bucket, s3Prefix);
    }

    private String getKmsCmkId(String path, S3StoreService s3StoreService) {
        Map<String, String> metadata = s3StoreService.getS3ObjectUserMetaData(path);
        if (! metadata.containsKey("x-amz-matdesc")) {
            throw new RuntimeException("Failed to get Customer Master Key ID from object user metadata. " +
                    "'x-amz-matdesc' not found in metadata for object at path: " + path);
        }

        String serializedEncryptionContext = metadata.get("x-amz-matdesc");

        Map<String, String> encryptionContextMap;
        try {
            encryptionContextMap = objectMapper.readValue(serializedEncryptionContext,
                    new TypeReference<HashMap<String,String>>() {});
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

    /**
     * Process the stored backup json
     * Step 1: Restores the metadata to CMS
     * Step 2: Restores the secrete data to Vault
     * @param sdbBackupJson the json string from s3
     */
    protected void processBackup(String sdbBackupJson, CerberusAdminClient cerberusAdminClient) throws IOException {

        JsonNode sdb = objectMapper.readTree(sdbBackupJson);

        // restore metadata to cms
        cerberusAdminClient.restoreMetadata(sdbBackupJson);
        deleteAllSecrets(sdb.get("path").asText(), cerberusAdminClient);
        // restore secret vault data
        JsonNode data = sdb.get("data");
        Map<String, Map<String, JsonNode>> kvPairs = objectMapper.convertValue(data,
                new TypeReference<HashMap<String,Map<String, JsonNode>>>() {});

        kvPairs.forEach((String path, Map<String, JsonNode> secretData) -> {
            Map<String, Object> genericDataMap = new HashMap<>();
            secretData.forEach((String key , JsonNode valueNode)-> {
                if (valueNode.isObject()) {
                    genericDataMap.put(key , objectMapper.convertValue(valueNode,
                            new TypeReference<HashMap<Object,Object>>() {})
                    );
                } else if (valueNode.isTextual()) {
                    genericDataMap.put(key , valueNode.textValue());
                } else if (valueNode.isBoolean()) {
                    genericDataMap.put(key , valueNode.booleanValue());
                } else {
                    throw new RuntimeException("Unexpected value type for secret value. Type: " + valueNode.getClass());
                }
            });
            cerberusAdminClient.writeJson(path, genericDataMap);
        });
    }

    /**
     * Deletes all of the secrets from Vault stored at the safe deposit box's path.
     *
     * @param path path to start deleting at.
     */
    protected void deleteAllSecrets(final String path, VaultAdminClient vaultAdminClient) {
        try {
            String fixedPath = path;

            if (StringUtils.endsWith(path, "/")) {
                fixedPath = StringUtils.substring(path, 0, StringUtils.lastIndexOf(path, "/"));
            }

            final VaultListResponse listResponse = vaultAdminClient.list(fixedPath);
            final List<String> keys = listResponse.getKeys();

            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (final String key : keys) {
                if (StringUtils.endsWith(key, "/")) {
                    final String fixedKey = StringUtils.substring(key, 0, key.lastIndexOf("/"));
                    deleteAllSecrets(fixedPath + "/" + fixedKey, vaultAdminClient);
                } else {
                    vaultAdminClient.delete(fixedPath + "/" + key);
                }
            }
        }  catch (VaultClientException vce) {
            throw new RuntimeException("Failed to delete secrets from Vault. for path: " + path);
        }
    }

    void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    void setS3Prefix(String s3Prefix) {
        this.s3Prefix = s3Prefix;
    }

    void setCerberusUrl(String cerberusUrl) {
        this.cerberusUrl = cerberusUrl;
    }
}
