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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Inject;
import com.nike.cerberus.client.CerberusAdminClient;
import com.nike.cerberus.command.core.RestoreCompleteCerberusDataFromS3BackupCommand;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.StaticVaultUrlResolver;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultTokenAuthRequest;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Operation for restoring Safe Deposit Box Metadata and Vault secret data for SDBs from backups that are in S3 from
 * the cross region backup lambda.
 */
public class RestoreCompleteCerberusDataFromS3BackupOperation implements Operation<RestoreCompleteCerberusDataFromS3BackupCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_TIMEOUT = 60;
    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;
    private static final String CERBERUS_BACKUP_METADATA_JSON_FILE_KEY = "cerberus-backup-metadata.json";
    private static final String CERBERUS_BACKUP_API_URL = "cerberusUrl";
    private static final String CERBERUS_BACKUP_DATE = "backupDate";
    private static final String CERBERUS_BACKUP_LAMBDA_ACCOUNT_ID = "lambdaBackupAccountId";
    private static final String CERBERUS_BACKUP_LAMBDA_REGION = "lambdaBackupRegion";
    private static final String CERBERUS_BACKUP_SDB_COUNT = "numberOfSdbs";

    private final VaultAdminClientFactory vaultAdminClientFactory;
    private final ObjectMapper objectMapper;
    private final ConsoleService console;

    @Inject
    public RestoreCompleteCerberusDataFromS3BackupOperation(VaultAdminClientFactory vaultAdminClientFactory,
                                                            @Named(CerberusModule.CONFIG_OBJECT_MAPPER)
                                                                    ObjectMapper objectMapper,
                                                            ConsoleService console) {

        this.vaultAdminClientFactory = vaultAdminClientFactory;
        this.objectMapper = objectMapper;
        this.console = console;
    }

    @Override
    public void run(RestoreCompleteCerberusDataFromS3BackupCommand command) {
        String backup = Chalk.on(String.format("s3://%s/%s",
                command.getS3Bucket(),
                command.getS3Prefix())
        ).yellow().bold().toString();

        logger.info(Chalk.on(
                String.format("Starting restore with backup located at %s", backup)
        ).green().toString());
        Region region = Region.getRegion(Regions.fromName(command.getS3Region()));

        AmazonS3 s3 = new AmazonS3Client();
        s3.setRegion(region);
        S3StoreService s3StoreService = new S3StoreService(s3, command.getS3Bucket(), command.getS3Prefix());

        Set<String> keys = s3StoreService.getKeysInPartialPath("");
        if (keys.isEmpty()) {
            logger.error("There where no keys in {}/{}", command.getS3Bucket(), command.getS3Prefix());
        }

        if (! keys.contains(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY)) {
            throw new RuntimeException(
                    String.format("cerberus-backup-metadata.json was not found in s3://%s/%s/ is this a complete backup?",
                            command.getS3Bucket(), command.getS3Prefix()));
        }

        String kmsCustomerMasterKeyId = getKmsCmkId(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        S3StoreService s3EncryptionStoreService = getS3EncryptionStoreService(kmsCustomerMasterKeyId, command);
        CerberusAdminClient cerberusAdminClient = new CerberusAdminClient(
                new StaticVaultUrlResolver(command.getCerberusUrl()),
                new VaultAdminClientFactory.RootCredentialsProvider(generateAdminToken()),
                new OkHttpClient.Builder()
                        .hostnameVerifier(new NoopHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
                        .build()
        );

        validateRestore(s3EncryptionStoreService, command);

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

    /**
     * Use the metadata from the backup and ensure that the user wants to proceed
     * @param s3StoreService - The encrypted S3 store service
     */
    private void validateRestore(S3StoreService s3StoreService, RestoreCompleteCerberusDataFromS3BackupCommand command) {
        String backupMetadataJsonString = getDecryptedJson(CERBERUS_BACKUP_METADATA_JSON_FILE_KEY, s3StoreService);
        Map<String, String> backupMetadata;
        try {
            backupMetadata = objectMapper.readValue(backupMetadataJsonString, new TypeReference<HashMap<String,String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize backup metadata", e);
        }

        String backupApiUrl = backupMetadata.containsKey(CERBERUS_BACKUP_API_URL) ? backupMetadata.get(CERBERUS_BACKUP_API_URL) : "unknown";
        String backupDate = backupMetadata.containsKey(CERBERUS_BACKUP_DATE) ? backupMetadata.get(CERBERUS_BACKUP_DATE) : "unknown";
        String backupLambdaAccountId = backupMetadata.containsKey(CERBERUS_BACKUP_LAMBDA_ACCOUNT_ID) ? backupMetadata.get(CERBERUS_BACKUP_LAMBDA_ACCOUNT_ID) : "unknown";
        String backupLambdaRegion = backupMetadata.containsKey(CERBERUS_BACKUP_LAMBDA_REGION) ? backupMetadata.get(CERBERUS_BACKUP_LAMBDA_REGION) : "unknown";
        String backupSdbCount = backupMetadata.containsKey(CERBERUS_BACKUP_SDB_COUNT) ? backupMetadata.get(CERBERUS_BACKUP_SDB_COUNT) : "unknown";

        StringBuilder msg = new StringBuilder()
                .append("\nThe backup you are attempting to restore was created from ").append(Chalk.on(backupApiUrl).green().toString()).append(" on ").append(Chalk.on(backupDate).green().bold().toString())
                .append("\nFrom the backup lambda in account ")
                .append(Chalk.on(backupLambdaAccountId).green().toString())
                .append(" in region ")
                .append(Chalk.on(backupLambdaRegion).green().toString())
                .append("\nThis backup contains ")
                .append(Chalk.on(backupSdbCount).green().toString())
                .append(" SDB records. ")
                .append("\nYou are attempting to restore this backup to ")
                .append(Chalk.on(command.getCerberusUrl()).green().toString());

        if (! backupApiUrl.equalsIgnoreCase(command.getCerberusUrl())) {
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

        if (! proceed.equalsIgnoreCase("proceed")) {
            throw new RuntimeException("User did not confirm to proceed with backup restore");
        }
    }

    private S3StoreService getS3EncryptionStoreService(String cmkId,
                                                       RestoreCompleteCerberusDataFromS3BackupCommand command) {

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

    /**
     * Generates and admin token that CMS will reconize as an Admin so we can us the restore endpoint
     */
    private String generateAdminToken() {
        VaultAuthResponse vaultAuthResponse;
        try {
            logger.info("Attempting to generate an admin token with the root token");
            VaultAdminClient adminClient = vaultAdminClientFactory.getClientForLeader().get();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("is_admin", "true");
            metadata.put("username", "admin-cli");

            Set<String> policies = new HashSet<>();
            policies.add("root");

            vaultAuthResponse = adminClient.createOrphanToken(new VaultTokenAuthRequest()
                    .setDisplayName("admin-cli")
                    .setPolicies(policies)
                    .setMeta(metadata)
                    .setTtl("10m")
                    .setNoDefaultPolicy(true));
        } catch (VaultClientException e) {
            throw new RuntimeException("There was an error while trying to create an admin token, this command " +
                    "requires proxy access or direct a connect to the vault leader, is your ip white listed?", e);
        }

        return vaultAuthResponse.getClientToken();
    }

    @Override
    public boolean isRunnable(RestoreCompleteCerberusDataFromS3BackupCommand command) {
        return true;
    }

}
