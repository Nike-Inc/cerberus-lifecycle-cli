/*
 * Copyright (c) 2016 Nike Inc.
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

package com.nike.cerberus.store;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.CmsOutputs;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.ConsulOutputs;
import com.nike.cerberus.domain.cloudformation.ConsulParameters;
import com.nike.cerberus.domain.cloudformation.GatewayOutputs;
import com.nike.cerberus.domain.cloudformation.GatewayParameters;
import com.nike.cerberus.domain.cloudformation.VaultOutputs;
import com.nike.cerberus.domain.cloudformation.VaultParameters;
import com.nike.cerberus.domain.configuration.ConsulConfiguration;
import com.nike.cerberus.domain.configuration.GatewayConfiguration;
import com.nike.cerberus.domain.configuration.VaultAclEntry;
import com.nike.cerberus.domain.configuration.VaultConfiguration;
import com.nike.cerberus.domain.environment.Environment;
import com.nike.cerberus.domain.environment.Secrets;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.domain.environment.CloudFrontLogProcessingLambdaConfig;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.IdentityManagementService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.service.StoreService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.ConfigConstants.CERT_PART_CA;
import static com.nike.cerberus.ConfigConstants.CERT_PART_CERT;
import static com.nike.cerberus.ConfigConstants.CERT_PART_KEY;
import static com.nike.cerberus.ConfigConstants.CERT_PART_PUBKEY;
import static com.nike.cerberus.module.CerberusModule.CF_OBJECT_MAPPER;
import static com.nike.cerberus.module.CerberusModule.CONFIG_OBJECT_MAPPER;

/**
 * Abstraction for accessing the configuration storage bucket.
 */
public class ConfigStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper configObjectMapper;

    private final ObjectMapper cloudFormationObjectMapper;

    private final IdentityManagementService iamService;

    private final AmazonS3 s3Client;

    private final EnvironmentMetadata environmentMetadata;

    private final Object envDataLock = new Object();

    private final Object secretsDataLock = new Object();

    private StoreService encryptedConfigStoreService;

    private StoreService configStoreService;

    @Inject
    public ConfigStore(final AmazonS3 s3Client,
                       final CloudFormationService cloudFormationService,
                       final IdentityManagementService iamService,
                       final EnvironmentMetadata environmentMetadata,
                       @Named(CONFIG_OBJECT_MAPPER) final ObjectMapper configObjectMapper,
                       @Named(CF_OBJECT_MAPPER) final ObjectMapper cloudFormationObjectMapper) {
        this.cloudFormationService = cloudFormationService;
        this.iamService = iamService;
        this.configObjectMapper = configObjectMapper;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.s3Client = s3Client;
        this.environmentMetadata = environmentMetadata;
    }

    /**
     * Writes the AZs to the configuration store.  This is a one time operation.  Subsequent calls will result in
     * an {@link IllegalStateException} being thrown.
     *
     * @param az1 AZ 1
     * @param az2 AZ 2
     * @param az3 AZ 3
     */
    public void storeAzs(final String az1, final String az2, final String az3) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();

            if (StringUtils.isNotBlank(environment.getAz1())
                    || StringUtils.isNotBlank(environment.getAz2())
                    || StringUtils.isNotBlank(environment.getAz3())) {
                throw new IllegalStateException("AZs are already defined for this environment!  Aborting save...");
            }

            environment.setAz1(az1);
            environment.setAz2(az2);
            environment.setAz3(az3);
            saveEnvironmentData(environment);
        }
    }

    /**
     * Stores the stack ID for a specific component.
     *
     * @param stackName Stack component
     * @param stackId Stack ID
     */
    public void storeStackId(final StackName stackName, final String stackId) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.getStackMap().put(stackName, stackId);
            saveEnvironmentData(environment);
        }
    }

    /**
     * Get the specific stack ID by component name.
     *
     * @param stackName Stack name
     * @return Stack ID
     */
    public String getStackId(final StackName stackName) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getStackMap().get(stackName);
        }
    }

    /**
     * Gets the server certificate name from the config store.
     *
     * @param stackName Stack name
     */
    public String getServerCertificateName(final StackName stackName) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getServerCertificateIdMap().get(stackName);
        }
    }

    /**
     * Gets the server certificate ARN for the stack name.
     *
     * @param stackName Stack name
     * @return ARN
     */
    public Optional<String> getServerCertificateArn(final StackName stackName) {
        final String certificateName = getServerCertificateName(stackName);
        return iamService.getServerCertificateArn(certificateName);
    }

    public Optional<String> getServerCertificateId(final StackName stackName) {
        final String certificateName = getServerCertificateName(stackName);
        return iamService.getServerCertificateId(certificateName);
    }

    /**
     * Stores the KMS key ID used to encrypt files in the config bucket.  This is a one time operation that will result
     * in {@link IllegalStateException} being thrown on subsequent calls.
     *
     * @param configKeyId The KMS key ID
     */
    public void storeConfigKeyId(final String configKeyId) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();

            if (StringUtils.isNotBlank(environment.getConfigKeyId())) {
                throw new IllegalStateException("Config Key ID is already defined for this environment!  Aborting save...");
            }

            environment.setConfigKeyId(configKeyId);
            saveEnvironmentData(environment);
        }
    }

    /**
     * Gets the replication bucket name from the config store.
     *
     * @return Replication bucket name
     */
    public String getReplicationBucketName() {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getReplicationBucketName();
        }
    }

    /**
     * Stores the replication bucket name in the config store.  If its already defined an {@link IllegalStateException}
     * is thrown.
     *
     * @param bucketName Replication bucket name
     */
    public void storeReplicationBucketName(final String bucketName) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();

            if (StringUtils.isNotBlank(environment.getReplicationBucketName())) {
                throw new IllegalStateException("Replication bucket name is already defined for this environment!  Aborting save...");
            }

            environment.setReplicationBucketName(bucketName);
            saveEnvironmentData(environment);
        }
    }

    /**
     * Checks if the consul configuration is present in the config store.
     *
     * @return If present
     */
    public boolean hasConsulConfig() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();

            return (StringUtils.isNotBlank(secrets.getConsul().getAclMasterToken())
                    && StringUtils.isNotBlank(secrets.getConsul().getGossipEncryptionToken()));
        }
    }

    /**
     * Uploads the configuration files and saves the secrets to the config store.
     *
     * @param consulConfiguration Consul configuration to upload
     */
    public void storeConsulConfig(final ConsulConfiguration consulConfiguration) {
        saveEncryptedObject(ConfigConstants.CONSUL_SERVER_CONFIG_FILE, consulConfiguration.getServerConfiguration());
        saveEncryptedObject(ConfigConstants.CONSUL_CLIENT_CONFIG_FILE, consulConfiguration.getClientConfiguration());

        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getConsul().setAclMasterToken(consulConfiguration.getInput().getAclMasterToken());
            secrets.getConsul().setGossipEncryptionToken(consulConfiguration.getInput().getGossipEncryptionToken());
            saveSecretsData(secrets);
        }
    }

    /**
     * Checks if the Vault ACL entry JSON is present in the config store.
     *
     * @return If present
     */
    public boolean hasVaultAclEntry() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();

            return StringUtils.isNotBlank(secrets.getConsul().getVaultAclToken());
        }
    }

    /**
     * Uploads the Vault ACL entry JSON and saves the secrets to the config store.
     *
     * @param vaultAclEntry Vaul ACL entry to upload
     */
    public void storeVaultAclEntry(final VaultAclEntry vaultAclEntry) {
        saveEncryptedObject(ConfigConstants.VAULT_ACL_ENTRY_FILE, vaultAclEntry.getEntry());

        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getConsul().setVaultAclToken(vaultAclEntry.getAclToken());
            saveSecretsData(secrets);
        }
    }

    /**
     * Retrieves the Vault ACL token for accessing Consul from the config store.
     *
     * @return Vault ACL token
     */
    public String getVaultAclToken() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            return secrets.getConsul().getVaultAclToken();
        }
    }

    /**
     * Checks if the Vault configuration file has already been uploaded to the config store.
     *
     * @return If present
     */
    public boolean hasVaultConfig() {
        final Optional<String> vaultConfig = getEncryptedObject(ConfigConstants.VAULT_CONFIG_FILE);
        return vaultConfig.isPresent();
    }

    /**
     * Uploads the Vault configuration to the config store.
     *
     * @param vaultConfiguration Vault configuration to upload.
     */
    public void storeVaultConfig(VaultConfiguration vaultConfiguration) {
        saveEncryptedObject(ConfigConstants.VAULT_CONFIG_FILE, vaultConfiguration.getConfig());
    }

    /**
     * Checks if the Gateway configuration file has already been uploaded to the config store.
     *
     * @return If present
     */
    public boolean hasGatewayConfig() {
        final Optional<String> gatewaySiteConfig = getEncryptedObject(ConfigConstants.GATEWAY_SITE_CONFIG_FILE);
        final Optional<String> gatewayGlobalConfig = getEncryptedObject(ConfigConstants.GATEWAY_GLOBAL_CONFIG_FILE);
        return gatewaySiteConfig.isPresent() && gatewayGlobalConfig.isPresent();
    }

    /**
     * Uploads the Gateway configuration to the config store.
     *
     * @param gatewayConfiguration Gateway configuration to upload.
     */
    public void storeGatewayConfig(GatewayConfiguration gatewayConfiguration) {
        saveEncryptedObject(ConfigConstants.GATEWAY_SITE_CONFIG_FILE, gatewayConfiguration.getSiteConfig());
        saveEncryptedObject(ConfigConstants.GATEWAY_GLOBAL_CONFIG_FILE, gatewayConfiguration.getGlobalConfig());
    }

    /**
     * Retrieves the CMS adming group from the config store.
     *
     * @return CMS admin group
     */
    public Optional<String> getCmsAdminGroup() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            return Optional.ofNullable(secrets.getCms().getAdminGroup());
        }
    }

    /**
     * Stores the CMS admin group.
     *
     * @param adminGroup Admin Group
     */
    public void storeCmsAdminGroup(final String adminGroup) {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getCms().setAdminGroup(adminGroup);
            saveSecretsData(secrets);
        }
    }

    /**
     * Retrieves the CMS database password from the config store.
     *
     * @return CMS database password
     */
    public Optional<String> getCmsDatabasePassword() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            return Optional.ofNullable(secrets.getCms().getDatabasePassword());
        }
    }

    /**
     * Stores the CMS database password.
     *
     * @param databasePassword Database password
     */
    public void storeCmsDatabasePassword(final String databasePassword) {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getCms().setDatabasePassword(databasePassword);
            saveSecretsData(secrets);
        }
    }

    /**
     * Retrieves the CMS Vault token from the config store.
     *
     * @return CMS Vault token
     */
    public Optional<String> getCmsVaultToken() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            return Optional.ofNullable(secrets.getCms().getVaultToken());
        }
    }

    /**
     * Stores the CMS Vault token.
     *
     * @param cmsVaultToken CMS Vault token
     */
    public void storeCmsVaultToken(final String cmsVaultToken) {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getCms().setVaultToken(cmsVaultToken);
            saveSecretsData(secrets);
        }
    }

    /**
     * Store just the Vault unseal keys.
     *
     * @param vaultKeys Vault unseal keys
     */
    public void storeVaultKeys(final List<String> vaultKeys) {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getVault().setKeys(vaultKeys);
            saveSecretsData(secrets);
        }
    }

    /**
     * Gets the Vault keys from the config store.
     *
     * @return List of Vault keys
     */
    public List<String> getVaultKeys() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            return secrets.getVault().getKeys();
        }
    }

    /**
     * Store just the Vault root token.
     *
     * @param vaultRootToken Vault root token
     */
    public void storeVaultRootToken(final String vaultRootToken) {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            secrets.getVault().setRootToken(vaultRootToken);
            saveSecretsData(secrets);
        }
    }

    /**
     * Gets the Vault root token from the config store.
     *
     * @return Vault root token
     */
    public String getVaultRootToken() {
        synchronized (secretsDataLock) {
            final Secrets secrets = getSecretsData();
            final String rootToken = secrets.getVault().getRootToken();
            return (rootToken != null) ? rootToken : "";
        }
    }

    /**
     * Returns the contents of a specific certificate part that's been uploaded for a stack.
     *
     * @param stackName
     * @param part
     * @return
     */
    public Optional<String> getCertPart(final StackName stackName, final String part) {
        return getEncryptedObject(buildCertFilePath(stackName, part));
    }

    /**
     * Stores the certificate files encrypted and adds the certificate name to the environment data.
     *
     * @param stackName Stack that the cert is for
     * @param certificateName Certificate name in IAM
     * @param caContents CA chain
     * @param certContents Certificate body
     * @param keyContents Certificate key
     * @param pubKeyContents Certificate public key
     */
    public void storeCert(final StackName stackName,
                          final String certificateName,
                          final String caContents,
                          final String certContents,
                          final String keyContents,
                          final String pubKeyContents) {
        saveEncryptedObject(buildCertFilePath(stackName, CERT_PART_CA), caContents);
        saveEncryptedObject(buildCertFilePath(stackName, CERT_PART_CERT), certContents);
        saveEncryptedObject(buildCertFilePath(stackName, CERT_PART_KEY), keyContents);
        saveEncryptedObject(buildCertFilePath(stackName, CERT_PART_PUBKEY), pubKeyContents);

        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.getServerCertificateIdMap().put(stackName, certificateName);
            saveEnvironmentData(environment);
        }
    }

    public void storeCmsEnvConfig(final Map<String, String> cmsConfigMap) {
        final StringBuilder cmsConfigContents = new StringBuilder();

        for (final Map.Entry<String, String> entry : cmsConfigMap.entrySet()) {
            cmsConfigContents.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

        saveEncryptedObject(ConfigConstants.CMS_ENV_CONFIG_PATH, cmsConfigContents.toString());
    }

    public Optional<String> getCmsEnvConfig() {
        return getEncryptedObject(ConfigConstants.CMS_ENV_CONFIG_PATH);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public BaseParameters getBaseStackParameters() {
        return getStackParameters(StackName.BASE, BaseParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public BaseOutputs getBaseStackOutputs() {
        return getStackOutputs(StackName.BASE, BaseOutputs.class);
    }

    /**
     * Get the Consul stack parameters.
     *
     * @return Consul parameters
     */
    public ConsulParameters getConsulStackParameters() {
        return getStackParameters(StackName.CONSUL, ConsulParameters.class);
    }

    /**
     * Get the Consul stack outputs.
     *
     * @return Consul outputs
     */
    public ConsulOutputs getConsulStackOutputs() {
        return getStackOutputs(StackName.CONSUL, ConsulOutputs.class);
    }

    /**
     * Get the Vault stack parameters.
     *
     * @return Vault parameters
     */
    public VaultParameters getVaultStackParamters() {
        return getStackParameters(StackName.VAULT, VaultParameters.class);
    }

    /**
     * Get the Vault stack outputs.
     *
     * @return Vault outputs
     */
    public VaultOutputs getVaultStackOutputs() {
        return getStackOutputs(StackName.VAULT, VaultOutputs.class);
    }

    /**
     * Get the CMS stack parameters.
     *
     * @return CMS parameters
     */
    public CmsParameters getCmsStackParamters() {
        return getStackParameters(StackName.CMS, CmsParameters.class);
    }

    /**
     * Get the CMS stack outputs.
     *
     * @return CMS outputs
     */
    public CmsOutputs getCmsStackOutputs() {
        return getStackOutputs(StackName.CMS, CmsOutputs.class);
    }

    /**
     * Get the Gateway stack parameters.
     *
     * @return Gateway parameters
     */
    public GatewayParameters getGatewayStackParamters() {
        return getStackParameters(StackName.GATEWAY, GatewayParameters.class);
    }

    /**
     * Get the Gateway stack outputs.
     *
     * @return Gateway outputs
     */
    public GatewayOutputs getGatewayStackOutputs() {
        return getStackOutputs(StackName.GATEWAY, GatewayOutputs.class);
    }

    /**
     * Get the stack outputs for a specific stack name.
     *
     * @param stackName Stack name
     * @param outputClass Outputs class
     * @param <M> Outputs type
     * @return Outputs
     */
    public <M> M getStackOutputs(final StackName stackName, final Class<M> outputClass) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            final String stackId = environment.getStackMap().get(stackName);

            if (!cloudFormationService.isStackPresent(stackId)) {
                throw new IllegalStateException("The specified stack doesn't exist for specified environment.");
            }

            final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);
            return cloudFormationObjectMapper.convertValue(stackOutputs, outputClass);
        }
    }

    /**
     * Get the stack parameters for a specific stack name.
     *
     * @param stackName Stack name
     * @param parameterClass Parameters class
     * @param <M> Parameters type
     * @return Parameters
     */
    public <M> M getStackParameters(final StackName stackName, final Class<M> parameterClass) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            final String stackId = environment.getStackMap().get(stackName);

            if (!cloudFormationService.isStackPresent(stackId)) {
                throw new IllegalStateException("The specified stack doesn't exist for the specified environment");
            }

            final Map<String, String> stackParameters = cloudFormationService.getStackParameters(stackId);
            return cloudFormationObjectMapper.convertValue(stackParameters, parameterClass);
        }
    }

    /**
     * Constructs the standard format for CNAMEs used by internal ELBs.
     *
     * @param stackName Stack name the CNAME is for
     * @return CNAME
     */
    public String getInternalElbCname(final StackName stackName) {
        final BaseParameters baseParameters = getBaseStackParameters();
        return String.format("%s.%s.%s.",
                stackName.getName(),
                environmentMetadata.getRegionName(),
                baseParameters.getVpcHostedZoneName());
    }

    /**
     * Initializes the environment data in the config bucket.
     */
    public void initEnvironmentData() {
        synchronized (envDataLock) {
            try {
                getEnvironmentData();
                final String errorMessage = "Attempting to initialize environment data, but it already exists!";
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } catch (IllegalStateException ise) {
                final Environment environment = new Environment();
                saveEnvironmentData(environment);
            }
        }
    }

    /**
     * Initializes the secrets data in the config bucket.
     */
    public void initSecretsData() {
        synchronized (secretsDataLock) {
            try {
                getSecretsData();
                final String errorMessage = "Attempting to initialize secrets data, but it already exists!";
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } catch (IllegalStateException ise) {
                final Secrets secrets = new Secrets();
                saveSecretsData(secrets);
            }
        }
    }

    private Secrets getSecretsData() {
        initEncryptedConfigStoreService();

        final Optional<String> secretsData = encryptedConfigStoreService.get(ConfigConstants.SECRETS_DATA_FILE);

        if (secretsData.isPresent()) {
            try {
                return configObjectMapper.readValue(secretsData.get(), Secrets.class);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the secrets data!", e);
            }
        } else {
            throw new IllegalStateException("No secrets data available!");
        }
    }

    private void saveSecretsData(final Secrets secrets) {
        try {
            final String secretsData = configObjectMapper.writeValueAsString(secrets);
            saveEncryptedObject(ConfigConstants.SECRETS_DATA_FILE, secretsData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert the environment data to JSON.  Aborting save...", e);
        }
    }

    private Environment getEnvironmentData() {
        initConfigStoreService();

        final Optional<String> envData = configStoreService.get(ConfigConstants.ENV_DATA_FILE);

        if (envData.isPresent()) {
            try {
                return configObjectMapper.readValue(envData.get(), Environment.class);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the environment data!", e);
            }
        } else {
            throw new IllegalStateException("No environment data available!");
        }
    }

    private void saveEnvironmentData(final Environment environment) {
        try {
            final String envData = configObjectMapper.writeValueAsString(environment);
            saveObject(ConfigConstants.ENV_DATA_FILE, envData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert the environment data to JSON.  Aborting save...", e);
        }
    }

    public void saveCFLogProcessorLambdaConfig(final CloudFrontLogProcessingLambdaConfig config) {
        try {
            final String configData = configObjectMapper.writeValueAsString(config);
            saveObject(ConfigConstants.CF_LOG_PROCESSOR_LAMBDA_CONFIG_FILE, configData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert the config data to JSON.  Aborting save...", e);
        }
    }

    private void saveObject(final String path, final String value) {
        initConfigStoreService();

        configStoreService.put(path, value);
    }

    private Optional<String> getObject(final String path) {
        initConfigStoreService();

        return configStoreService.get(path);
    }

    private void saveEncryptedObject(final String path, final String value) {
        initEncryptedConfigStoreService();

        encryptedConfigStoreService.put(path, value);
    }

    private Optional<String> getEncryptedObject(final String path) {
        initEncryptedConfigStoreService();

        return encryptedConfigStoreService.get(path);
    }

    private void initEncryptedConfigStoreService() {
        if (encryptedConfigStoreService == null) {
            final Environment environment = getEnvironmentData();

            KMSEncryptionMaterialsProvider materialProvider =
                    new KMSEncryptionMaterialsProvider(environment.getConfigKeyId());

            AmazonS3EncryptionClient encryptionClient =
                    new AmazonS3EncryptionClient(
                            new DefaultAWSCredentialsProviderChain(),
                            materialProvider,
                            new CryptoConfiguration()
                                    .withAwsKmsRegion(Region.getRegion(environmentMetadata.getRegions())))
                            .withRegion(Region.getRegion(environmentMetadata.getRegions()));

            encryptedConfigStoreService = new S3StoreService(encryptionClient, environmentMetadata.getBucketName(), "");
        }
    }

    private void initConfigStoreService() {
        if (configStoreService == null) {
            configStoreService = new S3StoreService(s3Client, environmentMetadata.getBucketName(), "");
        }
    }

    private String buildCertFilePath(final StackName stackName, final String suffix) {
        return "data/" + stackName.getName() + "/" + stackName.getName() + "-" + suffix;
    }
}
