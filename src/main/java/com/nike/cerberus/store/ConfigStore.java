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
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.CerberusEnvironmentData;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.*;
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
import java.util.LinkedList;

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
import static com.nike.cerberus.ConfigConstants.CMS_CERTIFICATE_TO_USE;
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

    private final AmazonS3 s3Client;

    private final EnvironmentMetadata environmentMetadata;

    private final SaltGenerator saltGenerator;

    private final Object cerberusEnvironmentDataLock = new Object();

    private StoreService encryptedConfigStoreService;

    private StoreService configStoreService;

    private AWSSecurityTokenService securityTokenService;

    @Inject
    public ConfigStore(final AmazonS3 s3Client,
                       final CloudFormationService cloudFormationService,
                       final AWSSecurityTokenService securityTokenService,
                       final EnvironmentMetadata environmentMetadata,
                       final SaltGenerator saltGenerator,
                       @Named(CONFIG_OBJECT_MAPPER) final ObjectMapper configObjectMapper,
                       final CloudFormationObjectMapper cloudFormationObjectMapper) {

        this.cloudFormationService = cloudFormationService;
        this.configObjectMapper = configObjectMapper;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.s3Client = s3Client;
        this.environmentMetadata = environmentMetadata;
        this.saltGenerator = saltGenerator;
        this.securityTokenService = securityTokenService;
    }

    /**
     * Retrieves the CMS database password from the config store.
     *
     * @return CMS database password
     */
    public Optional<String> getCmsDatabasePassword() {
        synchronized (cerberusEnvironmentDataLock) {
            final CerberusEnvironmentData environmentData = getDecryptedEnvironmentData();
            return Optional.ofNullable(environmentData.getDatabasePassword());
        }
    }

    /**
     * Stores the CMS database password.
     *
     * @param databasePassword Database password
     */
    public void storeCmsDatabasePassword(final String databasePassword) {
        synchronized (cerberusEnvironmentDataLock) {
            final CerberusEnvironmentData environmentData = getDecryptedEnvironmentData();
            environmentData.setDatabasePassword(databasePassword);
            saveEnvironmentData(environmentData);
        }
    }

    /**
     * Stores the certificate files encrypted and adds the certificate name to the environment data.
     *
     * @param certificateInformation Certificate information for cert
     * @param caContents             CA chain
     * @param certContents           Certificate body
     * @param keyContents            Certificate key
     * @param pubKeyContents         Certificate public key
     */
    public void storeCert(CertificateInformation certificateInformation,
                          String caContents,
                          String certContents,
                          String keyContents,
                          String pkcs8KeyContents,
                          String pubKeyContents) {

        String name = certificateInformation.getCertificateName();
        saveEncryptedObject(buildCertFilePath(name, CERT_PART_CA), caContents);
        saveEncryptedObject(buildCertFilePath(name, CERT_PART_CERT), certContents);
        saveEncryptedObject(buildCertFilePath(name, CERT_PART_KEY), keyContents);
        saveEncryptedObject(buildCertFilePath(name, CERT_PART_PKCS8_KEY), pkcs8KeyContents);
        saveEncryptedObject(buildCertFilePath(name, CERT_PART_PUBKEY), pubKeyContents);

        synchronized (cerberusEnvironmentDataLock) {
            final CerberusEnvironmentData environment = getDecryptedEnvironmentData();
            environment.addNewCertificateData(certificateInformation);

            saveEnvironmentData(environment);
        }
    }

    public LinkedList<CertificateInformation> getCertificationInformationList() {
        return getDecryptedEnvironmentData().getCertificateData();
    }

    /**
     * Deletes a set of cert and key files by certificate name
     * @param certificateName the name of the cert file bundle to delete
     */
    public void deleteCertificate(String certificateName) {
        initEncryptedConfigStoreService();

        encryptedConfigStoreService.deleteAllKeysOnPartialPath("certificates/" + certificateName);
        synchronized (cerberusEnvironmentDataLock) {
            final CerberusEnvironmentData environment = getDecryptedEnvironmentData();
            environment.removeCertificateInformationByName(certificateName);

            saveEnvironmentData(environment);
        }
    }

    /**
     * Returns the contents of a specific certificate part that's been uploaded for a stack.
     *
     * @param part
     * @return
     */
    public Optional<String> getCertPart(String certName, String part) {
        return getEncryptedObject(buildCertFilePath(certName, part));
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
        final String cmsDatabasePassword = getDecryptedEnvironmentData().getDatabasePassword();

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
        // Ust the latest uploaded certificate
        properties.put(CMS_CERTIFICATE_TO_USE, getCertificationInformationList().getLast().getCertificateName());

        return properties;
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

    /**
     * returns the complete stack name
     */
    public String getCloudFormationStackName(Stack stack) {
        return stack.getFullName(environmentMetadata.getName());
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public BaseParameters getBaseStackParameters() {
        return getStackParameters(getCloudFormationStackName(Stack.BASE), BaseParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public BaseOutputs getBaseStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.BASE), BaseOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public VpcParameters getVpcStackParameters() {
        return getStackParameters(getCloudFormationStackName(Stack.VPC), VpcParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public VpcOutputs getVpcStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.VPC), VpcOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public SecurityGroupParameters getSecurityGroupStackParameters() {
        return getStackParameters(getCloudFormationStackName(Stack.SECURITY_GROUPS), SecurityGroupParameters.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public SecurityGroupOutputs getSecurityGroupStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.SECURITY_GROUPS), SecurityGroupOutputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public LoadBalancerOutputs getLoadBalancerStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.LOAD_BALANCER), LoadBalancerOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public DatabaseParameters getDatabaseStackParameters() {
        return getStackParameters(getCloudFormationStackName(Stack.DATABASE), DatabaseParameters.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Parameters getRoute53Parameters() {
        return getStackParameters(getCloudFormationStackName(Stack.ROUTE53), Route53Parameters.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Outputs getRoute53StackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.ROUTE53), Route53Outputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public DatabaseOutputs getDatabaseStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.DATABASE), DatabaseOutputs.class);
    }

    /**
     * Get the CMS stack parameters.
     *
     * @return CMS parameters
     */
    public CmsParameters getCmsStackParamters() {
        return getStackParameters(getCloudFormationStackName(Stack.CMS), CmsParameters.class);
    }

    /**
     * Get the CMS stack outputs.
     *
     * @return CMS outputs
     */
    public CmsOutputs getCmsStackOutputs() {
        return getStackOutputs(getCloudFormationStackName(Stack.CMS), CmsOutputs.class);
    }

    /**
     * Get the Gateway stack parameters.
     *
     * @return Gateway parameters
     */
    public GatewayParameters getGatewayStackParameters() {
        return getStackParameters(getCloudFormationStackName(Stack.GATEWAY), GatewayParameters.class);
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
        if (!cloudFormationService.isStackPresent(stackName)) {
            throw new IllegalStateException("Failed to get CloudFormation output for stack: '" + stackName + "'. Stack does not exist.");
        }

        final Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(stackName);
        return cloudFormationObjectMapper.convertValue(stackOutputs, outputClass);
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
        if (!cloudFormationService.isStackPresent(stackName)) {
            throw new IllegalStateException("Failed to get CloudFormation parameters for stack: '" + stackName + "'. Stack does not exist.");
        }

        final Map<String, String> stackOutputs = cloudFormationService.getStackParameters(stackName);
        return cloudFormationObjectMapper.convertValue(stackOutputs, parameterClass);
    }

    /**
     * Initializes the environmentData data in the config bucket.
     */
    public void initEnvironmentData() {
        synchronized (cerberusEnvironmentDataLock) {
            try {
                getDecryptedEnvironmentData();
                final String errorMessage = "Attempting to initialize environmentData data, but it already exists!";
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } catch (IllegalStateException ise) {
                final CerberusEnvironmentData environmentData = new CerberusEnvironmentData();
                saveEnvironmentData(environmentData);
            }
        }
    }

    private CerberusEnvironmentData getDecryptedEnvironmentData() {
        initEncryptedConfigStoreService();

        final Optional<String> environmentData = encryptedConfigStoreService.get(ConfigConstants.ENVIRONMENT_DATA_FILE);

        if (environmentData.isPresent()) {
            try {
                return configObjectMapper.readValue(environmentData.get(), CerberusEnvironmentData.class);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the environment data!", e);
            }
        } else {
            throw new IllegalStateException("No environment data available!");
        }
    }

    private void saveEnvironmentData(final CerberusEnvironmentData environmentData) {
        try {
            final String environmentDataData = configObjectMapper.writeValueAsString(environmentData);
            saveEncryptedObject(ConfigConstants.ENVIRONMENT_DATA_FILE, environmentDataData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert the environment data to JSON.  Aborting save...", e);
        }
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

    private String buildCertFilePath(String identityManagementCertName, String filename) {
        return String.format("certificates/%s/%s", identityManagementCertName, filename);
    }

    public Optional<String> getMetricsTopicArn() {
        synchronized (cerberusEnvironmentDataLock) {
            CerberusEnvironmentData environment = getDecryptedEnvironmentData();
            if (StringUtils.isNoneBlank(environment.getMetricsTopicArn())) {
                return Optional.of(environment.getMetricsTopicArn());
            }
        }

        Optional<String> metricsTopicArn = cloudFormationService.searchStacksForOutput(CERBERUS_METRICS_TOPIC_ARN_STACK_OUTPUT_KEY);
        metricsTopicArn.ifPresent(this::storeMetricsTopicArn);

        return metricsTopicArn;
    }

    private void storeMetricsTopicArn(String arn) {
        synchronized (cerberusEnvironmentDataLock) {
            CerberusEnvironmentData environment = getDecryptedEnvironmentData();
            environment.setMetricsTopicArn(arn);
            saveEnvironmentData(environment);
        }
    }
}
