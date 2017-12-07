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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.nike.cerberus.command.CerberusCommand;
import com.nike.cerberus.command.ProxyDelegate;
import com.nike.cerberus.domain.environment.RegionDeserializer;
import com.nike.cerberus.domain.environment.RegionKeyDeserializer;
import com.nike.cerberus.domain.environment.RegionKeySerializer;
import com.nike.cerberus.domain.environment.RegionSerializer;
import com.nike.cerberus.domain.input.EnvironmentInput;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Guice module for enabling DI.
 */
public class CerberusModule extends AbstractModule {

    public static final String CONFIG_OBJECT_MAPPER = "configObjectMapper";
    public static final String ENV_NAME = "environmentName";
    public static final String CONFIG_REGION = "configRegionName";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyDelegate proxyDelegate;

    private final String environmentName;

    private final String configRegionName;

    private final EnvironmentInput environmentInput;

    public CerberusModule(CerberusCommand cerberusCommand) {
        proxyDelegate = cerberusCommand.getProxyDelegate();
        environmentName = cerberusCommand.getEnvironmentName();
        configRegionName = cerberusCommand.getConfigRegion();
        environmentInput = cerberusCommand.getEnvironmentInput();
    }

    /**
     * Binds all the Amazon services used.
     */
    @Override
    protected void configure() {
        // If a environment yaml was provided make it injectable as an Optional
        OptionalBinder.newOptionalBinder(binder(), EnvironmentInput.class);
        bind(EnvironmentInput.class).toProvider(Providers.of(environmentInput));

        bindConstant().annotatedWith(Names.named(ENV_NAME)).to(environmentName);
        bindConstant().annotatedWith(Names.named(CONFIG_REGION)).to(configRegionName);

        // bind the aws client factories
        bindAwsClientFactories();
    }

    private void bindAwsClientFactories() {
        bind(new TypeLiteral<AwsClientFactory<AmazonEC2Client>>() {}).toInstance(new AwsClientFactory<AmazonEC2Client>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonCloudFormationClient>>() {}).toInstance(new AwsClientFactory<AmazonCloudFormationClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonIdentityManagementClient>>() {}).toInstance(new AwsClientFactory<AmazonIdentityManagementClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AWSKMSClient>>() {}).toInstance(new AwsClientFactory<AWSKMSClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonS3Client>>() {}).toInstance(new AwsClientFactory<AmazonS3Client>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonAutoScalingClient>>() {}).toInstance(new AwsClientFactory<AmazonAutoScalingClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AWSSecurityTokenServiceClient>>() {}).toInstance(new AwsClientFactory<AWSSecurityTokenServiceClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AWSLambdaClient>>() {}).toInstance(new AwsClientFactory<AWSLambdaClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonSNSClient>>() {}).toInstance(new AwsClientFactory<AmazonSNSClient>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonRoute53Client>>() {}).toInstance(new AwsClientFactory<AmazonRoute53Client>() {});
        bind(new TypeLiteral<AwsClientFactory<AmazonElasticLoadBalancingClient>>() {}).toInstance(new AwsClientFactory<AmazonElasticLoadBalancingClient>() {});
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
        om.registerModule(new JodaModule());
        om.registerModule(new Jdk8Module());

        SimpleModule cerberusCustom = new SimpleModule();
        cerberusCustom.addSerializer(Regions.class, new RegionSerializer());
        cerberusCustom.addDeserializer(Regions.class, new RegionDeserializer());
        cerberusCustom.addKeyDeserializer(Regions.class, new RegionKeyDeserializer());
        cerberusCustom.addKeySerializer(Regions.class, new RegionKeySerializer());

        om.registerModule(cerberusCustom);
        om.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    @Provides
    @Singleton
    public MustacheFactory mustacheFactory() {
        return new DefaultMustacheFactory();
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

}
