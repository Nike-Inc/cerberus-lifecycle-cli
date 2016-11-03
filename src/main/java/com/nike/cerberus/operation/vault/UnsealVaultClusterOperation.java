package com.nike.cerberus.operation.vault;

import com.nike.cerberus.command.vault.UnsealVaultClusterCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Operation to unseal the Vault cluster.
 */
public class UnsealVaultClusterOperation implements Operation<UnsealVaultClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public UnsealVaultClusterOperation(final ConfigStore configStore,
                                       final VaultAdminClientFactory vaultAdminClientFactory) {
        this.configStore = configStore;
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(final UnsealVaultClusterCommand command) {
        logger.info("Getting Vault keys from configuration bucket.");
        final List<String> vaultKeys = configStore.getVaultKeys();

        logger.info("Getting clients for Vault instances.");
        final List<VaultAdminClient> clients = vaultAdminClientFactory.getClientsForCluster();

        logger.info("Unsealing each Vault instance.");
        clients.forEach(client ->
            vaultKeys.forEach(vaultKey -> client.unseal(vaultKey, false))
        );

        logger.info("Unsealing complete.");
    }

    @Override
    public boolean isRunnable(final UnsealVaultClusterCommand command) {
        return !vaultAdminClientFactory.getClientsForCluster().isEmpty();
    }
}
