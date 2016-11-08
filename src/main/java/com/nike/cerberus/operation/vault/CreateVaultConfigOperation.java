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
import com.nike.cerberus.command.vault.CreateVaultConfigCommand;
import com.nike.cerberus.domain.configuration.VaultConfiguration;
import com.nike.cerberus.generator.VaultConfigGenerator;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Operation that generates and uploads the Vault configuration.
 */
public class CreateVaultConfigOperation implements Operation<CreateVaultConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaultConfigGenerator vaultConfigGenerator;

    private final ConfigStore configStore;

    @Inject
    public CreateVaultConfigOperation(final VaultConfigGenerator vaultConfigGenerator,
                                      final ConfigStore configStore) {
        this.vaultConfigGenerator = vaultConfigGenerator;
        this.configStore = configStore;
    }

    @Override
    public void run(final CreateVaultConfigCommand command) {
        logger.info("Retrieving configuration data from the configuration bucket.");
        final String vaultAclToken = configStore.getVaultAclToken();

        logger.info("Generating the Vault configuration.");
        final VaultConfiguration vaultConfiguration =
                vaultConfigGenerator.generate(ConfigConstants.CONSUL_DATACENTER, vaultAclToken);

        logger.info("Uploading the Vault configuration to the configuration bucket.");
        configStore.storeVaultConfig(vaultConfiguration);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateVaultConfigCommand command) {
        boolean isRunnable = true;
        final String vaultAclToken = configStore.getVaultAclToken();
        final boolean hasVaultConfig = configStore.hasVaultConfig();

        if (StringUtils.isBlank(vaultAclToken)) {
            logger.error("No Vault ACL token present for Consul, please generate that first.");
            isRunnable = false;
        }

        if (hasVaultConfig) {
            logger.error("Vault configuration present for specified environment, use the update command.");
            isRunnable = false;
        }

        return isRunnable;
    }
}
