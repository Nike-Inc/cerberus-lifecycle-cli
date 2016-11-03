package com.nike.cerberus.operation.consul;

import com.nike.cerberus.command.consul.CreateVaultAclCommand;
import com.nike.cerberus.domain.configuration.VaultAclEntry;
import com.nike.cerberus.generator.VaultAclGenerator;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Operation that will generate the Vault ACL token for accessing Consul and upload it to the config store.
 */
public class CreateVaultAclOperation implements Operation<CreateVaultAclCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaultAclGenerator vaultAclGenerator;

    private final ConfigStore configStore;

    @Inject
    public CreateVaultAclOperation(final VaultAclGenerator vaultAclGenerator,
                                       final ConfigStore configStore) {
        this.vaultAclGenerator = vaultAclGenerator;
        this.configStore = configStore;
    }

    @Override
    public void run(final CreateVaultAclCommand command) {
        logger.info("Generating the Vault ACL entry.");
        final VaultAclEntry vaultAclEntry =
                vaultAclGenerator.generate();

        logger.info("Uploading the Vault ACL entry to the configuration bucket.");
        configStore.storeVaultAclEntry(vaultAclEntry);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateVaultAclCommand command) {
        final boolean hasVaultAclEntry = configStore.hasVaultAclEntry();

        if (hasVaultAclEntry) {
            logger.error("Vault ACL entry is present for specified environment, use the update command.");
        }

        return !hasVaultAclEntry;
    }
}
