package com.nike.cerberus.generator;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Inject;
import com.nike.cerberus.domain.configuration.GatewayConfiguration;
import com.nike.cerberus.domain.template.GatewayConfigurationInput;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Handles generating the Gateway configuration files.
 */
public class GatewayConfigGenerator {

    private final String siteConfigTemplatePath = "templates/site-gateway.conf.mustache";

    private final String globalConfigTemplatePath = "templates/nginx.conf.mustache";

    private final MustacheFactory mustacheFactory;

    @Inject
    public GatewayConfigGenerator(final MustacheFactory mustacheFactory) {
        this.mustacheFactory = mustacheFactory;
    }

    /**
     * Generates a configuration object that contains the newly generated gossip encryption token,
     * acl master token and a string representation of the full JSON configuration for Consul.
     */
    public GatewayConfiguration generate(final GatewayConfigurationInput input) {
        final Mustache siteConfigTemplateCompiler = mustacheFactory.compile(siteConfigTemplatePath);
        final Mustache globalConfigTemplateCompiler = mustacheFactory.compile(globalConfigTemplatePath);
        final StringWriter siteConfigWriter = new StringWriter();
        final StringWriter globalConfigWriter = new StringWriter();

        try {
            siteConfigTemplateCompiler.execute(siteConfigWriter, input).flush();
            globalConfigTemplateCompiler.execute(globalConfigWriter, input).flush();
        } catch (IOException ioe) {
            throw new ConfigGenerationException("Failed to generate Consul configuration!", ioe);
        }

        return new GatewayConfiguration()
                .setSiteConfig(siteConfigWriter.toString())
                .setGlobalConfig(globalConfigWriter.toString());
    }
}
