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

package com.nike.cerberus.module;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.ProxyDelegate;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.util.TokenSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Guice module for enabling DI.
 */
public class CerberusModule extends AbstractModule {

    public static final String CF_OBJECT_MAPPER = "cloudformationObjectMapper";

    public static final String CONFIG_OBJECT_MAPPER = "configObjectMapper";

    public static final String CERBERUS_ASSUME_ROLE_ARN = "CERBERUS_ASSUME_ROLE_ARN";

    public static final String CERBERUS_ASSUME_ROLE_EXTERNAL_ID = "CERBERUS_ASSUME_ROLE_EXTERNAL_ID";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyDelegate proxyDelegate;

    private final String environmentName;

    private final String regionName;

    public CerberusModule(ProxyDelegate proxyDelegate, String environmentName, String regionName) {
        this.proxyDelegate = proxyDelegate;
        this.environmentName = environmentName;
        this.regionName = regionName;
    }

    /**
     * Binds all the Amazon services used.
     */
    @Override
    protected void configure() {
        final Region region = Region.getRegion(Regions.fromName(regionName));
        bind(AmazonEC2.class).toInstance(createAmazonClientInstance(AmazonEC2Client.class, region));
        bind(AmazonCloudFormation.class).toInstance(createAmazonClientInstance(AmazonCloudFormationClient.class, region));
        bind(AmazonIdentityManagement.class).toInstance(createAmazonClientInstance(AmazonIdentityManagementClient.class, region));
        bind(AWSKMS.class).toInstance(createAmazonClientInstance(AWSKMSClient.class, region));
        bind(AmazonS3.class).toInstance(createAmazonClientInstance(AmazonS3Client.class, region));
        bind(AmazonAutoScaling.class).toInstance(createAmazonClientInstance(AmazonAutoScalingClient.class, region));
        bind(AWSSecurityTokenService.class).toInstance(createAmazonClientInstance(AWSSecurityTokenServiceClient.class, region));
        bind(AWSLambda.class).toInstance(createAmazonClientInstance(AWSLambdaClient.class, region));
        bind(AmazonSNS.class).toInstance(createAmazonClientInstance(AmazonSNSClient.class, region));
    }

    /**
     * Object mapper for handling CloudFormation parameters and outputs.
     *
     * @return Object mapper
     */
    @Provides
    @Singleton
    @Named(CF_OBJECT_MAPPER)
    public ObjectMapper cloudFormationObjectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return om;
    }

    /**
     * Object mapper for handling configuration objects in the config bucket.
     *
     * @return Object mapper
     */
    @Provides
    @Singleton
    @Named(CONFIG_OBJECT_MAPPER)
    public static ObjectMapper configObjectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        om.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    /**
     * Environment metadata object for describing the environment being executed against.
     *
     * @return Environment metadata
     */
    @Provides
    @Singleton
    public EnvironmentMetadata environmentMetadata() {
        final Optional<String> bucketName = findBucket(environmentName);
        final EnvironmentMetadata environmentMetadata = new EnvironmentMetadata(environmentName, regionName);

        if (bucketName.isPresent()) {
            environmentMetadata.setBucketName(bucketName.get());
        } else {
            logger.warn("Unable to determine the environment bucket for {}.", environmentName);
        }

        return environmentMetadata;
    }

    @Provides
    @Singleton
    public MustacheFactory mustacheFactory() {
        return new DefaultMustacheFactory();
    }

    @Provides
    @Singleton
    public TokenSupplier tokenSupplier() {
        return new TokenSupplier();
    }

    @Provides
    @Singleton
    public UuidSupplier uuidSupplier() {
        return new UuidSupplier();
    }

    @Provides
    @Singleton
    public Proxy proxy() {
        final Proxy.Type type = proxyDelegate.getProxyType();

        if (type == null || type == Proxy.Type.DIRECT) {
            return Proxy.NO_PROXY;
        }

        final String host = proxyDelegate.getProxyHost();
        final Integer port = proxyDelegate.getProxyPort();

        if (StringUtils.isBlank(host) || port == null) {
            logger.warn("Invalid proxy settings, ignoring...");
            return Proxy.NO_PROXY;
        }

        return new Proxy(type, new InetSocketAddress(host, port));
    }

    private Optional<String> findBucket(final String environmentName) {
        AmazonS3Client s3Client = new AmazonS3Client();
        List<Bucket> buckets = s3Client.listBuckets();

        String envBucket = null;
        for (final Bucket bucket : buckets) {
            if (StringUtils.contains(bucket.getName(), ConfigConstants.CONFIG_BUCKET_KEY)) {
                String[] parts = bucket.getName().split("-");
                if (StringUtils.equalsIgnoreCase(environmentName, parts[0])) {
                    envBucket = bucket.getName();
                    break;
                }
            }
        }

        return Optional.ofNullable(envBucket);
    }

    private static <M extends AmazonWebServiceClient> M createAmazonClientInstance(Class<M> clientClass, Region region) {
        return region.createClient(clientClass, getAWSCredentialsProviderChain(), new ClientConfiguration());
    }

    public static AWSCredentialsProviderChain getAWSCredentialsProviderChain() {
        String cerberusRoleToAssume = System.getenv(CERBERUS_ASSUME_ROLE_ARN) != null ?
                System.getenv(CERBERUS_ASSUME_ROLE_ARN) : "";
        String cerberusRoleToAssumeExternalId = System.getenv(CERBERUS_ASSUME_ROLE_EXTERNAL_ID) != null ?
                System.getenv(CERBERUS_ASSUME_ROLE_EXTERNAL_ID) : "";

        STSAssumeRoleSessionCredentialsProvider sTSAssumeRoleSessionCredentialsProvider =
                new STSAssumeRoleSessionCredentialsProvider
                        .Builder(cerberusRoleToAssume, UUID.randomUUID().toString())
                        .withExternalId(cerberusRoleToAssumeExternalId)
                        .build();

        AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(
                new EnvironmentVariableCredentialsProvider(),
                new SystemPropertiesCredentialsProvider(),
                new ProfileCredentialsProvider(),
                sTSAssumeRoleSessionCredentialsProvider,
                new InstanceProfileCredentialsProvider());

        return chain;
    }
}
