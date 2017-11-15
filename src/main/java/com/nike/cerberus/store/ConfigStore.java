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

package com.nike.cerberus.store;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.CmsOutputs;
import com.nike.cerberus.domain.cloudformation.CmsParameters;
import com.nike.cerberus.domain.cloudformation.DatabaseOutputs;
import com.nike.cerberus.domain.cloudformation.DatabaseParameters;
import com.nike.cerberus.domain.cloudformation.GatewayParameters;
import com.nike.cerberus.domain.cloudformation.LoadBalancerOutputs;
import com.nike.cerberus.domain.cloudformation.Route53Outputs;
import com.nike.cerberus.domain.cloudformation.Route53Parameters;
import com.nike.cerberus.domain.cloudformation.SecurityGroupOutputs;
import com.nike.cerberus.domain.cloudformation.SecurityGroupParameters;
import com.nike.cerberus.domain.cloudformation.VaultOutputs;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.BackupRegionInfo;
import com.nike.cerberus.domain.environment.Environment;
import com.nike.cerberus.domain.environment.Secrets;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.IdentityManagementService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.service.SaltGenerator;
import com.nike.cerberus.service.StoreService;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static com.nike.cerberus.ConfigConstants.ADMIN_ROLE_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.CERT_PART_CA;
import static com.nike.cerberus.ConfigConstants.CERT_PART_CERT;
import static com.nike.cerberus.ConfigConstants.CERT_PART_KEY;
import static com.nike.cerberus.ConfigConstants.CERT_PART_PKCS8_KEY;
import static com.nike.cerberus.ConfigConstants.CERT_PART_PUBKEY;
import static com.nike.cerberus.ConfigConstants.CMS_ENV_NAME;
import static com.nike.cerberus.ConfigConstants.CMS_ROLE_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.HASH_SALT;
import static com.nike.cerberus.ConfigConstants.JDBC_PASSWORD_KEY;
import static com.nike.cerberus.ConfigConstants.JDBC_URL_KEY;
import static com.nike.cerberus.ConfigConstants.JDBC_USERNAME_KEY;
import static com.nike.cerberus.ConfigConstants.ROOT_USER_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.SYSTEM_CONFIGURED_CMS_PROPERTIES;
import static com.nike.cerberus.module.CerberusModule.CONFIG_OBJECT_MAPPER;

/**
 * Abstraction for accessing the configuration storage bucket.
 */
public class ConfigStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CERBERUS_METRICS_TOPIC_ARN_STACK_OUTPUT_KEY = "CerberusMetricsTopicArn";

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper configObjectMapper;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final IdentityManagementService iamService;

    private final AmazonS3 s3Client;

    private final EnvironmentMetadata environmentMetadata;

    private final SaltGenerator saltGenerator;

    private final Object envDataLock = new Object();

    private final Object secretsDataLock = new Object();

    private StoreService encryptedConfigStoreService;

    private StoreService configStoreService;

    private AWSSecurityTokenService securityTokenService;

    @Inject
    public ConfigStore(final AmazonS3 s3Client,
                       final CloudFormationService cloudFormationService,
                       final IdentityManagementService iamService,
                       final AWSSecurityTokenService securityTokenService,
                       final EnvironmentMetadata environmentMetadata,
                       final SaltGenerator saltGenerator,
                       @Named(CONFIG_OBJECT_MAPPER) final ObjectMapper configObjectMapper,
                       final CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.cloudFormationService = cloudFormationService;
        this.iamService = iamService;
        this.configObjectMapper = configObjectMapper;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.s3Client = s3Client;
        this.environmentMetadata = environmentMetadata;
        this.saltGenerator = saltGenerator;
        this.securityTokenService = securityTokenService;
    }

    /**
     * Gets the server certificate name from the config store.
     *
     * @param stack Stack name
     */
    public String getServerCertificateName(final Stack stack) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getServerCertificateIdMap().get(stack);
        }
    }

    /**
     * Gets the server certificate ARN for the stack name.
     *
     * @param stack Stack name
     * @return ARN
     */
    public Optional<String> getServerCertificateArn(final Stack stack) {
        final String certificateName = getServerCertificateName(stack);
        return iamService.getServerCertificateArn(certificateName);
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
     * @param stack
     * @param part
     * @return
     */
    public Optional<String> getCertPart(final Stack stack, final String part) {
        return getEncryptedObject(buildCertFilePath(stack, part));
    }

    /**
     * Stores the certificate files encrypted and adds the certificate name to the environment data.
     *
     * @param stack           Stack that the cert is for
     * @param certificateName Certificate name in IAM
     * @param caContents      CA chain
     * @param certContents    Certificate body
     * @param keyContents     Certificate key
     * @param pubKeyContents  Certificate public key
     */
    public void storeCert(final Stack stack,
                          final String certificateName,
                          final String caContents,
                          final String certContents,
                          final String keyContents,
                          final String pkcs8KeyContents,
                          final String pubKeyContents) {

        saveEncryptedObject(buildCertFilePath(stack, CERT_PART_CA), caContents);
        saveEncryptedObject(buildCertFilePath(stack, CERT_PART_CERT), certContents);
        saveEncryptedObject(buildCertFilePath(stack, CERT_PART_KEY), keyContents);
        saveEncryptedObject(buildCertFilePath(stack, CERT_PART_PKCS8_KEY), pkcs8KeyContents);
        saveEncryptedObject(buildCertFilePath(stack, CERT_PART_PUBKEY), pubKeyContents);

        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.getServerCertificateIdMap().put(stack, certificateName);
            saveEnvironmentData(environment);
        }
    }

    public void storeCmsEnvConfig(final Properties cmsConfigMap) {
        final StringBuilder cmsConfigContents = new StringBuilder();

        cmsConfigMap.keySet().stream().forEach(key -> {
            cmsConfigContents.append(key).append('=').append(cmsConfigMap.get(key)).append('\n');
        });

        saveEncryptedObject(ConfigConstants.CMS_ENV_CONFIG_PATH, cmsConfigContents.toString());
    }

    public Optional<String> getCmsEnvConfig() {
        return getEncryptedObject(ConfigConstants.CMS_ENV_CONFIG_PATH);
    }

    /**
     * Get the CMS environment properties
     *
     * @return CMS properties
     */
    public Properties getAllExistingCmsEnvProperties() {

        Properties properties = new Properties();
        Optional<String> config = getCmsEnvConfig();

        if (config.isPresent()) {
            try {
                properties.load(new StringReader(config.get()));
            } catch (IOException ioe) {
                throw new IllegalStateException("Failed to read CMS properties");
            }
        }

        return properties;
    }

    /**
     * Generate CMS properties that are not set by the user
     *
     * @return - System configured properties
     */
    private Properties generateBaseCmsSystemProperties() {

        final BaseOutputs baseOutputs = getBaseStackOutputs();
        final DatabaseOutputs databaseOutputs = getDatabaseStackOutputs();
        final BaseParameters baseParameters = getBaseStackParameters();
        final String cmsDatabasePassword = getSecretsData().getCms().getDatabasePassword();

        final GetCallerIdentityResult callerIdentity = securityTokenService.getCallerIdentity(
                new GetCallerIdentityRequest());
        final String rootUserArn = String.format("arn:aws:iam::%s:root", callerIdentity.getAccount());

        final Properties properties = new Properties();
        properties.put(ROOT_USER_ARN_KEY, rootUserArn);
        properties.put(ADMIN_ROLE_ARN_KEY, baseParameters.getAccountAdminArn());
        properties.put(CMS_ROLE_ARN_KEY, baseOutputs.getCmsIamRoleArn());
        properties.put(JDBC_URL_KEY, databaseOutputs.getCmsDbJdbcConnectionString());
        properties.put(JDBC_USERNAME_KEY, ConfigConstants.DEFAULT_CMS_DB_NAME);
        properties.put(JDBC_PASSWORD_KEY, cmsDatabasePassword);
        properties.put(CMS_ENV_NAME, environmentMetadata.getName());

        return properties;
    }

    public Optional<String> getAccountAdminArn() {
        final BaseParameters baseParameters = getBaseStackParameters();
        return Optional.ofNullable(baseParameters.getAccountAdminArn());
    }

    public String getCerberusBaseUrl() {
        return String.format("https://%s", getEnvironmentData().getDomainName());
    }

    /**
     * System properties not set with -P param
     */
    public Properties getCmsSystemProperties() {

        Properties properties = new Properties();
        Properties existingProperties = getAllExistingCmsEnvProperties();

        // overwrite any of the automatically generated properties that may have changed
        existingProperties.putAll(generateBaseCmsSystemProperties());

        existingProperties.forEach((key, value) -> {
            if (SYSTEM_CONFIGURED_CMS_PROPERTIES.contains(key)) {
                properties.put(key, value);
            }
        });

        if (!properties.containsKey(HASH_SALT)) {
            properties.put(HASH_SALT, saltGenerator.generateSalt());
        }

        return properties;
    }

    /**
     * Get existing CMS properties configured by the user
     *
     * @return - User configured properties
     */
    public Properties getExistingCmsUserProperties() {

        Properties properties = new Properties();
        Properties existingProperties = getAllExistingCmsEnvProperties();

        existingProperties.forEach((key, value) -> {
            if (!SYSTEM_CONFIGURED_CMS_PROPERTIES.contains(key)) {
                properties.put(key, value);
            }
        });

        return properties;
    }

    /**
     * Return configuration file contents
     *
     * @param path - Path to configuration file (e.g. 'config/environment.properties')
     */
    public Optional<String> getConfigProperties(String path) {

        return getEncryptedObject(path);
    }

    public String getStackLogicalId(Stack stack) {
        return stack.getFullName(environmentMetadata.getName());
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public BaseParameters getBaseStackParameters() {
        return getStackParameters(getStackLogicalId(Stack.BASE), BaseParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public BaseOutputs getBaseStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.BASE), BaseOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public VpcParameters getVpcStackParameters() {
        return getStackParameters(getStackLogicalId(Stack.VPC), VpcParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public VpcOutputs getVpcStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.VPC), VpcOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public SecurityGroupParameters getSecurityGroupStackParameters() {
        return getStackParameters(getStackLogicalId(Stack.SECURITY_GROUPS), SecurityGroupParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public SecurityGroupOutputs getSecurityGroupStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.SECURITY_GROUPS), SecurityGroupOutputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public LoadBalancerOutputs getLoadBalancerStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.LOAD_BALANCER), LoadBalancerOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public DatabaseParameters getDatabaseStackParameters() {
        return getStackParameters(getStackLogicalId(Stack.DATABASE), DatabaseParameters.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Parameters getRoute53Parameters() {
        return getStackParameters(getStackLogicalId(Stack.ROUTE53), Route53Parameters.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Outputs getRoute53StackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.ROUTE53), Route53Outputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public DatabaseOutputs getDatabaseStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.DATABASE), DatabaseOutputs.class);
    }

    /**
     * Get the Vault stack outputs.
     *
     * @return Vault outputs
     */
    public VaultOutputs getVaultStackOutputs() {
        return getStackOutputs(Stack.VAULT, VaultOutputs.class);
    }

    /**
     * Get the CMS stack parameters.
     *
     * @return CMS parameters
     */
    public CmsParameters getCmsStackParamters() {
        return getStackParameters(getStackLogicalId(Stack.CMS), CmsParameters.class);
    }

    /**
     * Get the CMS stack outputs.
     *
     * @return CMS outputs
     */
    public CmsOutputs getCmsStackOutputs() {
        return getStackOutputs(getStackLogicalId(Stack.CMS), CmsOutputs.class);
    }

    /**
     * Get the Gateway stack parameters.
     *
     * @return Gateway parameters
     */
    public GatewayParameters getGatewayStackParamters() {
        return getStackParameters(Stack.GATEWAY, GatewayParameters.class);
    }

    /**
     * Get the stack outputs for a specific stack name.
     *
     * @param stack       Stack name
     * @param outputClass Outputs class
     * @param <M>         Outputs type
     * @return Outputs
     */
    public <M> M getStackOutputs(final Stack stack, final Class<M> outputClass) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            final String stackId = environment.getStackMap().get(stack);

            if (!cloudFormationService.isStackPresent(stackId)) {
                throw new IllegalStateException("The specified stack doesn't exist for specified environment.");
            }

            final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);
            return cloudFormationObjectMapper.convertValue(stackOutputs, outputClass);
        }
    }

    /**
     * Get the stack outputs for a specific stack name.
     *
     * @param stackName   Full stack name
     * @param outputClass Outputs class
     * @param <M>         Outputs type
     * @return Outputs
     */
    public <M> M getStackOutputs(final String stackName, final Class<M> outputClass) {
        final String stackId = Stack.fromName(stackName).getFullName(environmentMetadata.getName());

        if (!cloudFormationService.isStackPresent(stackId)) {
            throw new IllegalStateException("Failed to get CloudFormation output for stack: '" + stackName + "'. Stack does not exist.");
        }

        final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackId);
        return cloudFormationObjectMapper.convertValue(stackOutputs, outputClass);
    }

    /**
     * Get the stack parameters for a specific stack name.
     *
     * @param stack          Stack name
     * @param parameterClass Parameters class
     * @param <M>            Parameters type
     * @return Parameters
     */
    public <M> M getStackParameters(final Stack stack, final Class<M> parameterClass) {
        synchronized (envDataLock) {
            final String stackId = stack.getFullName(environmentMetadata.getName());

            if (!cloudFormationService.isStackPresent(stackId)) {
                throw new IllegalStateException("The specified stack doesn't exist for the specified environment");
            }

            final Map<String, String> stackParameters = cloudFormationService.getStackParameters(stackId);
            return cloudFormationObjectMapper.convertValue(stackParameters, parameterClass);
        }
    }

    /**
     * Get the stack parameters for a specific stack name.
     *
     * @param stackName      Full stack name
     * @param parameterClass Parameters class
     * @param <M>            Parameters type
     * @return Parameters
     */
    public <M> M getStackParameters(final String stackName, final Class<M> parameterClass) {
        final String stackId = Stack.fromName(stackName).getFullName(environmentMetadata.getName());

        if (!cloudFormationService.isStackPresent(stackId)) {
            throw new IllegalStateException("Failed to get CloudFormation parameters for stack: '" + stackName + "'. Stack does not exist.");
        }

        final Map<String, String> stackOutputs = cloudFormationService.getStackParameters(stackId);
        return cloudFormationObjectMapper.convertValue(stackOutputs, parameterClass);
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

    private void saveObject(final String path, final String value) {
        initConfigStoreService();

        configStoreService.put(path, value);
    }

    private Optional<String> getObject(final String path) {
        initConfigStoreService();

        return configStoreService.get(path);
    }

    /**
     * List under a path as-if it were a folder
     */
    public Set<String> listUnderPartialPath(final String path) {
        initConfigStoreService();

        return configStoreService.listUnderPartialPath(path);
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
            KMSEncryptionMaterialsProvider materialProvider =
                    new KMSEncryptionMaterialsProvider(getBaseStackOutputs().getConfigFileKeyId());

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

    private String buildCertFilePath(final Stack stack, final String suffix) {
        return "data/" + stack.getName() + "/" + stack.getName() + "-" + suffix;
    }

    /**
     * Removes the final '.' from the CNAME.
     *
     * @param cname The cname to convert
     * @return The host derived from the CNAME
     */
    private String cnameToHost(final String cname) {
        return cname.substring(0, cname.length() - 1);
    }

    public Optional<BackupRegionInfo> getBackupInfoForRegion(String region) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return Optional.ofNullable(environment.getRegionBackupBucketMap().get(region));
        }
    }

    public Map<String, BackupRegionInfo> getRegionBackupBucketMap() {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getRegionBackupBucketMap();
        }
    }

    public void storeBackupInfoForRegion(String region, String bucket, String kmsCmkId) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.getRegionBackupBucketMap().put(region, new BackupRegionInfo(bucket, kmsCmkId));
            saveEnvironmentData(environment);
        }
    }

    public Set<String> getBackupAdminIamPrincipals() {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            return environment.getBackupAdminIamPrincipals();
        }
    }

    public void storeBackupAdminIamPrincipals(Set<String> principals) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.setBackupAdminIamPrincipals(principals);
            saveEnvironmentData(environment);
        }
    }

    public Optional<String> getMetricsTopicArn() {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            if (StringUtils.isNoneBlank(environment.getMetricsTopicArn())) {
                return Optional.of(environment.getMetricsTopicArn());
            }
        }

        Optional<String> metricsTopicArn = cloudFormationService.searchStacksForOutput(CERBERUS_METRICS_TOPIC_ARN_STACK_OUTPUT_KEY);
        metricsTopicArn.ifPresent(this::storeMetricsTopicArn);

        return metricsTopicArn;
    }

    private void storeMetricsTopicArn(String arn) {
        synchronized (envDataLock) {
            final Environment environment = getEnvironmentData();
            environment.setMetricsTopicArn(arn);
            saveEnvironmentData(environment);
        }
    }

}
