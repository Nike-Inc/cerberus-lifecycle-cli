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

        boolean isRunnable = true;

        final Optional<String> cmsVaultToken = configStore.getCmsVaultToken();
        final boolean hasVaultInstances = vaultAdminClientFactory.hasVaultInstances();
        final Optional<VaultAdminClient> vaultAdminClient = vaultAdminClientFactory.getClientForLeader();

        if (command.isForceOverwrite()) {
            if (cmsVaultToken.isPresent()) {
                logger.warn("WARNING: Since " + CreateCmsVaultTokenCommand.FORCE_OVERWRITE_LONG_ARG + " was used there may be an old CMS Vault token that needs to be manually revoked in Vault. "
                        + "The old token can be looked up in the previous version of secrets.json stored in S3 in order to revoke it.");
            } else {
                // We don't have to exit here except this probably means someone doesn't understand --force-overwrite
                logger.error(CreateCmsVaultTokenCommand.FORCE_OVERWRITE_LONG_ARG + " was used but there is no existing CMS Vault token.  Please re-run without " + CreateCmsVaultTokenCommand.FORCE_OVERWRITE_LONG_ARG + " option.");
                isRunnable = false;
            }
        } else if (cmsVaultToken.isPresent()) {
            logger.error("CMS Vault token is already present.  Use update command to rotate it (or in a non-production system consider using the " + CreateCmsVaultTokenCommand.FORCE_OVERWRITE_LONG_ARG + " option)");
            isRunnable = false;
        }

        if (!hasVaultInstances) {
            logger.error("No vault instances present for this environment!");
            isRunnable = false;
        }

        if (!vaultAdminClient.isPresent()) {
            logger.error("No Vault instance is the current leader!");
            isRunnable = false;
        }

        return isRunnable;
    }
}
