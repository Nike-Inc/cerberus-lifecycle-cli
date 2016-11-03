package com.nike.cerberus.operation.vault;

import com.nike.cerberus.command.vault.DisableAuditBackendCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Handles disabling the audit backend specified in the command.
 */
public class DisableAuditBackendOperation implements Operation<DisableAuditBackendCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public DisableAuditBackendOperation(final VaultAdminClientFactory vaultAdminClientFactory) {
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(DisableAuditBackendCommand command) {
        logger.info("Getting client for Vault leader.");
        final Optional<VaultAdminClient> client = vaultAdminClientFactory.getClientForLeader();

        if (client.isPresent()) {
            logger.info("Disabling the specified audit backend in Vault.");
            client.get().disableAuditBackend(command.getAuditBackend().getType());
            logger.info("Audit disabled.");
        } else {
            throw new IllegalStateException("Unable to determine Vault leader, aborting...");
        }
    }

    @Override
    public boolean isRunnable(final DisableAuditBackendCommand command) {
        return vaultAdminClientFactory.getClientForLeader().isPresent();
    }
}
