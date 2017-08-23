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

import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.vault.InitVaultClusterCommand;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultInitResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
 * Operation for initializing each Vault instance in the cluster.
 */
public class InitVaultClusterOperation implements Operation<InitVaultClusterCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public InitVaultClusterOperation(final ConfigStore configStore,
                                     final VaultAdminClientFactory vaultAdminClientFactory) {
        this.configStore = configStore;
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(final InitVaultClusterCommand command) {
        logger.info("If you just created the Vault cluster, this command will fail until the Vault cluster has a chance to initialize.  If that happens, just try again in a few minutes.");

        logger.info("Getting clients for Vault instances.");
        final List<VaultAdminClient> clients = vaultAdminClientFactory.getClientsForCluster();

        logger.info("Calling init on one Vault instance.");
        final VaultInitResponse initResponse =
                clients.get(0).init(ConfigConstants.VAULT_SECRET_SHARES, ConfigConstants.VAULT_SECRET_THRESHOLD);

        logger.info("Uploading Vault root token and keys to configuration bucket.");
        configStore.storeVaultRootToken(initResponse.getRootToken());
        configStore.storeVaultKeys(initResponse.getKeys());

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final InitVaultClusterCommand command) {
        return StringUtils.isNotBlank(configStore.getStackId(StackName.VAULT));
    }
}
