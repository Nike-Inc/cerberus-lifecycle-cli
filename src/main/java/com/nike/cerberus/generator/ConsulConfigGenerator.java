package com.nike.cerberus.generator;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Inject;
import com.nike.cerberus.domain.configuration.ConsulConfiguration;
import com.nike.cerberus.domain.template.ConsulConfigurationInput;
import com.nike.cerberus.util.TokenSupplier;
import com.nike.cerberus.util.UuidSupplier;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Handles generating the Consul configuration files.
 */
public class ConsulConfigGenerator {

    private final String serverTemplatePath = "templates/consul-server.json.mustache";

    private final String clientTemplatePath = "templates/consul-client.json.mustache";

    private final UuidSupplier uuidSupplier;

    private final TokenSupplier tokenSupplier;

    private final MustacheFactory mustacheFactory;

    @Inject
    public ConsulConfigGenerator(final UuidSupplier uuidSupplier,
                                 final TokenSupplier tokenSupplier,
                                 final MustacheFactory mustacheFactory) {
        this.uuidSupplier = uuidSupplier;
        this.tokenSupplier = tokenSupplier;
        this.mustacheFactory = mustacheFactory;
    }

    /**
     * Generates a configuration object that contains the newly generated gossip encryption token,
     * acl master token and a string representation of the full JSON configuration for Consul.
     */
    public ConsulConfiguration generate(final String datacenter) {
        final String aclMasterToken = uuidSupplier.get();
        final String gossipEncryptionToken = tokenSupplier.get();
        final ConsulConfigurationInput input = new ConsulConfigurationInput()
                .setAclMasterToken(aclMasterToken)
                .setGossipEncryptionToken(gossipEncryptionToken)
                .setDatacenter(datacenter);
        final Mustache serverTemplateCompiler = mustacheFactory.compile(serverTemplatePath);
        final Mustache clientTemplateCompiler = mustacheFactory.compile(clientTemplatePath);
        final StringWriter serverConfigWriter = new StringWriter();
        final StringWriter clientConfigWriter = new StringWriter();

        try {
            serverTemplateCompiler.execute(serverConfigWriter, input).flush();
            clientTemplateCompiler.execute(clientConfigWriter, input).flush();
        } catch (IOException ioe) {
            throw new ConfigGenerationException("Failed to generate Consul configuration!", ioe);
        }

        return new ConsulConfiguration()
                .setInput(input)
                .setServerConfiguration(serverConfigWriter.toString())
                .setClientConfiguration(clientConfigWriter.toString());
    }
}
