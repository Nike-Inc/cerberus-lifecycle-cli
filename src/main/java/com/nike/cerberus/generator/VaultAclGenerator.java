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

    private final String vaultAclTemplatePath = "templates/vault-acl.json.mustache";

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
