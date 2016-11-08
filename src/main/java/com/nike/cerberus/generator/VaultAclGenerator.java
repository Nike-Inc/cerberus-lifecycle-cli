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

package com.nike.cerberus.generator;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Inject;
import com.nike.cerberus.domain.configuration.VaultAclEntry;
import com.nike.cerberus.domain.template.VaultAclInput;
import com.nike.cerberus.util.UuidSupplier;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Generates the Vault ACL entry and token.
 */
public class VaultAclGenerator {

    private static final String vaultAclTemplatePath = "templates/vault-acl.json.mustache";

    private final UuidSupplier uuidSupplier;

    private final MustacheFactory mustacheFactory;

    @Inject
    public VaultAclGenerator(final UuidSupplier uuidSupplier,
                             final MustacheFactory mustacheFactory) {
        this.uuidSupplier = uuidSupplier;
        this.mustacheFactory = mustacheFactory;
    }

    /**
     * Generates the Vault ACL token and entry JSON.
     *
     * @return POJO with the token and JSON
     */
    public VaultAclEntry generate() {
        final String aclToken = uuidSupplier.get();
        final VaultAclInput input = new VaultAclInput().setAclToken(aclToken);
        final Mustache vaultAclEntryCompiler = mustacheFactory.compile(vaultAclTemplatePath);
        final StringWriter vaulAclEntryStringWriter = new StringWriter();

        try {
            vaultAclEntryCompiler.execute(vaulAclEntryStringWriter, input).flush();
        } catch (final IOException ioe) {
            throw new ConfigGenerationException("Failed to generate Vault ACL entry!", ioe);
        }

        return new VaultAclEntry().setAclToken(aclToken).setEntry(vaulAclEntryStringWriter.toString());
    }
}
