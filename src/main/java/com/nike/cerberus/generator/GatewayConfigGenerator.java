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
import com.nike.cerberus.domain.configuration.GatewayConfiguration;
import com.nike.cerberus.domain.template.GatewayConfigurationInput;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Handles generating the Gateway configuration files.
 */
public class GatewayConfigGenerator {

    private static final String siteConfigTemplatePath = "templates/site-gateway.conf.mustache";

    private static final String globalConfigTemplatePath = "templates/nginx.conf.mustache";

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
