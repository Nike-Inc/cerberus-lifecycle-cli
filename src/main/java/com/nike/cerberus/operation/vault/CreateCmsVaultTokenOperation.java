package com.nike.cerberus.operation.vault;

import com.nike.cerberus.command.vault.CreateCmsVaultTokenCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultTokenAuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Operation to create the Vault token used by CMS.
 */
public class CreateCmsVaultTokenOperation implements Operation<CreateCmsVaultTokenCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String tokenDisplayName = "cerberus-management-service-token";

    private final ConfigStore configStore;

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public CreateCmsVaultTokenOperation(final ConfigStore configStore,
                                        final VaultAdminClientFactory vaultAdminClientFactory) {
        this.configStore = configStore;
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(final CreateCmsVaultTokenCommand command) {
        logger.info("Getting client for Vault leader.");
        final Optional<VaultAdminClient> vaultAdminClient = vaultAdminClientFactory.getClientForLeader();

        if (vaultAdminClient.isPresent()) {
            logger.info("Creating CMS Vault token.");
            final VaultAuthResponse authResponse = vaultAdminClient.get().createToken(
                    new VaultTokenAuthRequest().setDisplayName(tokenDisplayName));

            logger.info("Uploading CMS Vault token to configuration bucket.");
            configStore.storeCmsVaultToken(authResponse.getClientToken());

            logger.info("Uploading complete.");
        } else {
            throw new IllegalStateException("Unable to determine Vault leader, aborting...");
        }
    }

    @Override
    public boolean isRunnable(final CreateCmsVaultTokenCommand command) {
        final Optional<String> cmsVaultToken = configStore.getCmsVaultToken();
        final boolean hasVaultInstances = vaultAdminClientFactory.hasVaultInstances();
        final Optional<VaultAdminClient> vaultAdminClient = vaultAdminClientFactory.getClientForLeader();

        if (cmsVaultToken.isPresent()) {
            logger.error("CMS Vault token is already present.  Use update command to rotate it.");
        }

        if (!hasVaultInstances) {
            logger.error("No vault instances present for this environment!");
        }

        if (!vaultAdminClient.isPresent()) {
            logger.error("No Vault instance is the current leader!");
        }

        return !cmsVaultToken.isPresent() && hasVaultInstances && vaultAdminClient.isPresent();
    }
}
