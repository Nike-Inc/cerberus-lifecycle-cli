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

    private static final String serverTemplatePath = "templates/consul-server.json.mustache";

    private static final String clientTemplatePath = "templates/consul-client.json.mustache";

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
     * acl master token, and string representations of the full JSON configuration for Consul.
     */
    public ConsulConfiguration generate(final String datacenter) {
        final String aclMasterToken = uuidSupplier.get();
        final String gossipEncryptionToken = tokenSupplier.get();
        return generate(datacenter, aclMasterToken, gossipEncryptionToken);
    }

    /**
     * Generates a configuration object that contains the supplied gossip encryption token,
     * acl master token, and string representations of the full JSON configuration for Consul.
     */
    public ConsulConfiguration generate(final String datacenter,
                                        final String aclMasterToken,
                                        final String gossipEncryptionToken) {
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
