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

package com.nike.cerberus.store;

import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomaslanger.chalk.Chalk;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.cloudformation.IamRolesOutputs;
import com.nike.cerberus.domain.cloudformation.DatabaseOutputs;
import com.nike.cerberus.domain.cloudformation.ConfigOutputs;
import com.nike.cerberus.domain.cloudformation.Route53Outputs;
import com.nike.cerberus.domain.cloudformation.SecurityGroupOutputs;
import com.nike.cerberus.domain.cloudformation.VpcOutputs;
import com.nike.cerberus.domain.cloudformation.VpcParameters;
import com.nike.cerberus.domain.environment.EnvironmentData;
import com.nike.cerberus.domain.environment.CertificateInformation;
import com.nike.cerberus.domain.environment.RegionData;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.service.EncryptionService;
import com.nike.cerberus.service.S3StoreService;
import com.nike.cerberus.service.SaltGenerator;
import com.nike.cerberus.service.StoreService;
import com.nike.cerberus.util.CloudFormationObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static com.nike.cerberus.ConfigConstants.*;
import static com.nike.cerberus.module.CerberusModule.CONFIG_OBJECT_MAPPER;
import static com.nike.cerberus.module.CerberusModule.CONFIG_REGION;
import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Abstraction for accessing the configuration storage bucket and the environment state stored in it.
 */
@Singleton
public class ConfigStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AwsClientFactory<AmazonS3Client> amazonS3ClientFactory;

    private final CloudFormationService cloudFormationService;

    private final ObjectMapper configObjectMapper;

    private final CloudFormationObjectMapper cloudFormationObjectMapper;

    private final SaltGenerator saltGenerator;

    private final AwsClientFactory<AWSSecurityTokenServiceClient> securityTokenServiceFactory;

    private final String environmentName;

    private final Regions configRegion;

    private final EncryptionService encryptionService;

    private Map<Regions, StoreService> storeServiceMap = new HashMap<>();

    @Inject
    public ConfigStore(AwsClientFactory<AmazonS3Client> amazonS3ClientFactory,
                       CloudFormationService cloudFormationService,
                       AwsClientFactory<AWSSecurityTokenServiceClient> securityTokenServiceFactory,
                       SaltGenerator saltGenerator,
                       @Named(CONFIG_OBJECT_MAPPER) ObjectMapper configObjectMapper,
                       CloudFormationObjectMapper cloudFormationObjectMapper,
                       @Named(ENV_NAME) String environmentName,
                       @Named(CONFIG_REGION) String configRegion,
                       EncryptionService encryptionService) {

        this.cloudFormationService = cloudFormationService;
        this.configObjectMapper = configObjectMapper;
        this.cloudFormationObjectMapper = cloudFormationObjectMapper;
        this.amazonS3ClientFactory = amazonS3ClientFactory;
        this.saltGenerator = saltGenerator;
        this.securityTokenServiceFactory = securityTokenServiceFactory;
        this.environmentName = environmentName;
        this.configRegion = Regions.fromName(configRegion);
        this.encryptionService = encryptionService;
    }

    /**
     * Retrieves the CMS database password from the config store.
     *
     * @return CMS database password
     */
    public Optional<String> getCmsDatabasePassword() {
        EnvironmentData environmentData = getDecryptedEnvironmentData();
        return Optional.ofNullable(environmentData.getDatabasePassword());
    }

    /**
     * Stores the CMS database password.
     *
     * @param databasePassword Database password
     */
    public void storeCmsDatabasePassword(String databasePassword) {
        EnvironmentData environmentData = getDecryptedEnvironmentData();
        environmentData.setDatabasePassword(databasePassword);
        saveEnvironmentData(environmentData);
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

        EnvironmentData environmentData = getDecryptedEnvironmentData();

        String name = certificateInformation.getCertificateName();
        encryptAndSaveObject(buildCertFilePath(name, CERT_PART_CA), caContents, environmentData);
        encryptAndSaveObject(buildCertFilePath(name, CERT_PART_CERT), certContents, environmentData);
        encryptAndSaveObject(buildCertFilePath(name, CERT_PART_KEY), keyContents, environmentData);
        encryptAndSaveObject(buildCertFilePath(name, CERT_PART_PKCS8_KEY), pkcs8KeyContents, environmentData);
        encryptAndSaveObject(buildCertFilePath(name, CERT_PART_PUBKEY), pubKeyContents, environmentData);

        environmentData.addNewCertificateData(certificateInformation);

        saveEnvironmentData(environmentData);
    }

    public Optional<KeyPair> getAcmeAccountKeyPair() {
        Optional<String> serializedKeyPair =
                getEncryptedObject(CERT_ACME_ACCOUNT_PRIVATE_KEY).map(encryptionService::decrypt);

        if (!serializedKeyPair.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(KeyPairUtils.readKeyPair(new StringReader(serializedKeyPair.get())));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read keypair from serialized data", e);
        }
    }

    public void storeAcmeUserKeyPair(KeyPair keyPair) {
        EnvironmentData environmentData = getDecryptedEnvironmentData();
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter jw = new JcaPEMWriter(stringWriter)) {
            jw.writeObject(keyPair);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write key pair", e);
        }
        encryptAndSaveObject(CERT_ACME_ACCOUNT_PRIVATE_KEY, stringWriter.toString(), environmentData);
    }

    public LinkedList<CertificateInformation> getCertificationInformationList() {
        return getDecryptedEnvironmentData().getCertificateData();
    }

    public Optional<CertificateInformation> getLastCert() {
        EnvironmentData environmentData = getDecryptedEnvironmentData();
        if (environmentData.getCertificateData().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(environmentData.getCertificateData().getLast());
    }

    /**
     * Deletes a set of cert and key files by certificate name
     * @param certificateName the name of the cert file bundle to delete
     */
    public void deleteCertificate(String certificateName) {
        String path = "certificates/" + certificateName;
        getDecryptedEnvironmentData().getConfigRegions().forEach(region -> {
            try {
                getStoreServiceForRegion(region, getDecryptedEnvironmentData()).deleteAllKeysOnPartialPath(path);
            } catch (Exception e) {
                logger.error(Chalk.on(
                        String.format("Failed to delete object at path: %s for region: %s, cross region data may be " +
                                "out of sync and require manual fixing", path, region)
                ).bold().red().toString());
            }
        });

        EnvironmentData environment = getDecryptedEnvironmentData();
        environment.removeCertificateInformationByName(certificateName);

        saveEnvironmentData(environment);
    }

    /**
     * Returns the contents of a specific certificate part that's been uploaded for a stack.
     *
     * @param part
     * @return
     */
    public Optional<String> getCertPart(String certName, String part) {
        return getEncryptedObject(buildCertFilePath(certName, part)).map(encryptionService::decrypt);
    }

    public void storeCmsEnvConfig(Properties cmsConfigMap) {
        StringBuilder cmsConfigContents = new StringBuilder();

        cmsConfigMap.keySet().forEach(key -> {
            cmsConfigContents.append(key).append('=').append(cmsConfigMap.get(key)).append('\n');
        });

        encryptAndSaveObject(ConfigConstants.CMS_ENV_CONFIG_PATH, cmsConfigContents.toString(), getDecryptedEnvironmentData());
    }

    public Optional<String> getCmsEnvConfig() {
        return getEncryptedObject(ConfigConstants.CMS_ENV_CONFIG_PATH).map(encryptionService::decrypt);
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
     * Generate the global CMS properties that are not set by the user
     *
     * @return - System configured properties
     */
    private Properties generateBaseCmsSystemProperties() {
        EnvironmentData data = getDecryptedEnvironmentData();
        String cmsDatabasePassword = data.getDatabasePassword();
        String cmsIamRoleArn = data.getCmsIamRoleArn();
        String adminRoleArn = data.getAdminIamRoleArn();
        String rootUserArn = data.getRootIamRoleArn();
        String jdbcUrl = getDatabaseStackOutputs(getPrimaryRegion()).getCmsDbJdbcConnectionString();

        Properties properties = new Properties();
        properties.put(ROOT_USER_ARN_KEY, rootUserArn);
        properties.put(ADMIN_ROLE_ARN_KEY, adminRoleArn);
        properties.put(CMS_ROLE_ARN_KEY, cmsIamRoleArn);
        properties.put(JDBC_URL_KEY, jdbcUrl);
        properties.put(JDBC_USERNAME_KEY, ConfigConstants.DEFAULT_CMS_DB_NAME);
        properties.put(JDBC_PASSWORD_KEY, cmsDatabasePassword);
        properties.put(CMS_ENV_NAME, environmentName);
        properties.put(CMS_CERTIFICATE_TO_USE, getCertificationInformationList().getLast().getCertificateName());
        properties.put(CMK_ARNS_KEY, StringUtils.join(data.getManagementServiceCmkArns(), ","));

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
        return getEncryptedObject(path).map(encryptionService::decrypt);
    }

    /**
     * returns the complete stack name
     */
    public String getCloudFormationStackName(Stack stack) {
        return stack.getFullName(environmentName);
    }

    /**
     * Get the cms iam role stack outputs for primary region
     *
     * @return Base outputs
     */
    public IamRolesOutputs getCmsIamRoleOutputs() {
        return getStackOutputs(getPrimaryRegion(), getCloudFormationStackName(Stack.IAM_ROLES), IamRolesOutputs.class);
    }

    /**
     * Get the cms iam role stack outputs for provided region
     *
     * @return Base outputs
     */
    public IamRolesOutputs getCmsIamRoleOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.IAM_ROLES), IamRolesOutputs.class);
    }

    /**
     * Get the cms iam role stack outputs.
     *
     * @return Base outputs
     */
    public ConfigOutputs getConfigBucketStackOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.CONFIG), ConfigOutputs.class);
    }

    /**
     * Get the base stack parameters for primary region.
     *
     * @return Base parameters
     */
    public VpcParameters getVpcStackParameters() {
        return getStackParameters(getPrimaryRegion(), getCloudFormationStackName(Stack.VPC), VpcParameters.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public VpcParameters getVpcStackParameters(Regions region) {
        return getStackParameters(region, getCloudFormationStackName(Stack.VPC), VpcParameters.class);
    }

    /**
     * Get the base stack outputs for primary region.
     *
     * @return Base outputs
     */
    public VpcOutputs getVpcStackOutputs() {
        return getStackOutputs(getPrimaryRegion(), getCloudFormationStackName(Stack.VPC), VpcOutputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public VpcOutputs getVpcStackOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.VPC), VpcOutputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public SecurityGroupOutputs getSecurityGroupStackOutputs() {
        return getStackOutputs(getPrimaryRegion(), getCloudFormationStackName(Stack.SECURITY_GROUPS), SecurityGroupOutputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public SecurityGroupOutputs getSecurityGroupStackOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.SECURITY_GROUPS), SecurityGroupOutputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Outputs getRoute53StackOutputs() {
        return getStackOutputs(getPrimaryRegion(), getCloudFormationStackName(Stack.ROUTE53), Route53Outputs.class);
    }

    /**
     * Get the base stack parameters.
     *
     * @return Base parameters
     */
    public Route53Outputs getRoute53StackOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.ROUTE53), Route53Outputs.class);
    }

    /**
     * Get the base stack outputs.
     *
     * @return Base outputs
     */
    public DatabaseOutputs getDatabaseStackOutputs(Regions region) {
        return getStackOutputs(region, getCloudFormationStackName(Stack.DATABASE), DatabaseOutputs.class);
    }

    /**
     * Get the stack outputs for a specific stack name.
     *
     * @param stackName   Full stack name
     * @param outputClass Outputs class
     * @param <M>         Outputs type
     * @return Outputs
     */
    public <M> M getStackOutputs(Regions region, String stackName, Class<M> outputClass) {
        if (!cloudFormationService.isStackPresent(region, stackName)) {
            throw new IllegalStateException("Failed to get CloudFormation output for stack: '" + stackName + "'. Stack does not exist.");
        }

        Map<String, String> stackOutputs = cloudFormationService.getStackOutputs(region, stackName);
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
    public <M> M getStackParameters(Regions region, String stackName, Class<M> parameterClass) {
        if (!cloudFormationService.isStackPresent(region, stackName)) {
            throw new IllegalStateException("Failed to get CloudFormation parameters for stack: '" + stackName + "'. Stack does not exist.");
        }

        Map<String, String> stackOutputs = cloudFormationService.getStackParameters(region, stackName);
        return cloudFormationObjectMapper.convertValue(stackOutputs, parameterClass);
    }

    private EnvironmentData getDecryptedEnvironmentData() {
        if (! storeServiceMap.containsKey(configRegion)) {
            String bucket = findConfigBucketInSuppliedConfigRegion();
            storeServiceMap.put(configRegion, new S3StoreService(amazonS3ClientFactory.getClient(configRegion), bucket, ""));
        }

        Optional<String> environmentData = storeServiceMap.get(configRegion).get(ConfigConstants.ENVIRONMENT_DATA_FILE);

        if (environmentData.isPresent()) {
            try {
                return configObjectMapper.readValue(encryptionService.decrypt(environmentData.get()), EnvironmentData.class);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the environment data!", e);
            }
        } else {
            throw new IllegalStateException("No environment data available!");
        }
    }

    private void saveEnvironmentData(EnvironmentData environmentData) {
        try {
            String serializedPlainTextEnvironmentData = configObjectMapper.writeValueAsString(environmentData);
            encryptAndSaveObject(ConfigConstants.ENVIRONMENT_DATA_FILE, serializedPlainTextEnvironmentData, environmentData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert the environment data to JSON.  Aborting save...", e);
        }
    }

    /**
     * List keys in config bucket under a path as-if it were a folder
     */
    public Set<String> listUnderPartialPath(String path) {
        return getStoreServiceForRegion(configRegion, getDecryptedEnvironmentData()).listUnderPartialPath(path);
    }

    private void encryptAndSaveObject(String path,
                                      String plaintextSerializedObject,
                                      EnvironmentData environmentData) {

        List<String> environmentDataKmsCmkArns = new LinkedList<>();
        environmentData.getRegionData().forEach((region, regionData) ->
                regionData.getConfigCmkArn().ifPresent(environmentDataKmsCmkArns::add));
        MasterKeyProvider<KmsMasterKey> encryptProvider = initializeKeyProvider(environmentDataKmsCmkArns);

        String encryptedObject = encryptionService.encrypt(encryptProvider, plaintextSerializedObject);

        environmentData.getConfigRegions().forEach(region -> {
            try {
                getStoreServiceForRegion(region, environmentData).put(path, encryptedObject);
            } catch (Exception e) {
                logger.error(Chalk.on(
                        String.format("Failed to save object at path: %s for region: %s, cross region data may be " +
                                "out of sync and require manual fixing", path, region)
                ).bold().red().toString());
            }
        });
    }

    private StoreService getStoreServiceForRegion(Regions region, EnvironmentData environmentData) {
        if (!storeServiceMap.containsKey(region)) {
            String bucket = environmentData.getRegionData().get(region).getConfigBucket()
                    .orElseThrow(() -> new RuntimeException("bucket not configured for region: " + region));
            storeServiceMap.put(region, new S3StoreService(amazonS3ClientFactory.getClient(region), bucket, ""));
        }
        return storeServiceMap.get(region);
    }

    /**
     * @param path the path to the encrypted text
     * @return The serialized cipher text from s3
     */
    private Optional<String> getEncryptedObject(String path) {
        return getStoreServiceForRegion(configRegion, getDecryptedEnvironmentData()).get(path);
    }

    private String buildCertFilePath(String identityManagementCertName, String filename) {
        return String.format("certificates/%s/%s", identityManagementCertName, filename);
    }

    private String findConfigBucketInSuppliedConfigRegion() {
        AmazonS3Client s3Client = amazonS3ClientFactory.getClient(configRegion);
        List<Bucket> buckets = s3Client.listBuckets();
        String envBucket = null;
        for (Bucket bucket : buckets) {
            String bucketName = bucket.getName();
            if (StringUtils.contains(bucket.getName(), ConfigConstants.CONFIG_BUCKET_KEY)) {
                String bucketLocationResult = s3Client.getBucketLocation(bucketName);
                Regions bucketRegion = StringUtils.equals("US", bucketLocationResult) ? Regions.US_EAST_1 : Regions.fromName(bucketLocationResult);
                if (configRegion.equals(bucketRegion)) {
                    String tokenizedEnvName = StringUtils.replaceAll(environmentName, "_", "-");
                    if (StringUtils.startsWith(bucketName, tokenizedEnvName)) {
                        envBucket = bucketName;
                        break;
                    }
                }
            }
        }

        if (StringUtils.isBlank(envBucket)) {
            throw new RuntimeException("Failed to find config bucket for region: " + configRegion);
        }

        return envBucket;
    }

    @SuppressWarnings("unchecked")
    private MasterKeyProvider<KmsMasterKey> initializeKeyProvider(List<String> cmkArns) {
        List<MasterKeyProvider<KmsMasterKey>> providers = cmkArns.stream()
                .map(KmsMasterKeyProvider::new)
                .collect(Collectors.toList());
        return (MasterKeyProvider<KmsMasterKey>) MultipleProviderFactory.buildMultiProvider(providers);
    }

    public Regions getPrimaryRegion() {
        return getDecryptedEnvironmentData().getPrimaryRegion();
    }

    public List<Regions> getConfigEnabledRegions() {
        return getDecryptedEnvironmentData().getConfigRegions();
    }

    /**
     * Initializes the config state for a Cerberus Environment, called once at environment creation
     *
     * @param adminRoleArn The admin role arn for the Cerberus environment, needed for KMS policies
     * @param cmsIamRoleArn The CMS role arn for the Cerberus environment, needed for KMS policies
     * @param primaryRegion The primary region that will serve traffic and have the data store and cms asg
     * @param regionConfigOutputsMap The outputs that have the config buckets and KMS CMKs for encrypting sensitive config
     */
    public void initializeEnvironment(String adminRoleArn,
                                      String cmsIamRoleArn,
                                      Regions primaryRegion,
                                      Map<Regions, ConfigOutputs> regionConfigOutputsMap) {

        AWSSecurityTokenService securityTokenService = securityTokenServiceFactory.getClient(configRegion);
        GetCallerIdentityResult callerIdentity = securityTokenService.getCallerIdentity(
                new GetCallerIdentityRequest());
        String rootUserArn = String.format("arn:aws:iam::%s:root", callerIdentity.getAccount());

        EnvironmentData environmentData = new EnvironmentData();
        environmentData.setEnvironmentName(environmentName);
        environmentData.setAdminIamRoleArn(adminRoleArn);
        environmentData.setCmsIamRoleArn(cmsIamRoleArn);
        environmentData.setRootIamRoleArn(rootUserArn);
        regionConfigOutputsMap.forEach((region, output) -> {
            RegionData regionData = new RegionData();
            if (region.equals(primaryRegion)) {
                regionData.setPrimary(true);
            }
            regionData.setConfigBucket(output.getConfigBucketName());
            regionData.setConfigCmkArn(output.getConfigCmkArn());
            regionData.setManagementServiceCmkArn(output.getManagementServiceCmkArn());
            environmentData.addRegionData(region, regionData);
        });

        saveEnvironmentData(environmentData);
    }

    public String getConfigBucketForRegion(Regions region) {
        if (!getDecryptedEnvironmentData().getRegionData().containsKey(region)) {
            throw new RuntimeException("There is no region data for region: " + region.getName());
        }
        RegionData data = getDecryptedEnvironmentData().getRegionData().get(region);
        return data.getConfigBucket().orElseThrow(() ->
                new RuntimeException("There is no config bucket configured for region: " + region.getName()));
    }

    public String getCmsCmkForRegion(Regions region) {
        if (!getDecryptedEnvironmentData().getRegionData().containsKey(region)) {
            throw new RuntimeException("There is no region data for region: " + region.getName());
        }
        RegionData data = getDecryptedEnvironmentData().getRegionData().get(region);
        return data.getManagementServiceCmkArn().orElseThrow(() ->
                new RuntimeException("There is no cms cmk configured for region: " + region.getName()));
    }

    public String getEnvironmentDataSecureDataKmsCmkRegion(Regions region) {
        if (!getDecryptedEnvironmentData().getRegionData().containsKey(region)) {
            throw new RuntimeException("There is no region data for region: " + region.getName());
        }
        RegionData data = getDecryptedEnvironmentData().getRegionData().get(region);
        return data.getConfigCmkArn().orElseThrow(() ->
                new RuntimeException("There is no cms cmk configured for region: " + region.getName()));
    }

    public EnvironmentData getEnvironmentData() {
        return getDecryptedEnvironmentData();
    }
}
