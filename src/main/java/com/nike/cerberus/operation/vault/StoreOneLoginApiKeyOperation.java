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

import com.google.common.collect.Maps;
import com.nike.cerberus.command.vault.StoreOneLoginApiKeyCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * Operation for storing the OneLogin API key which is required by CMS to operate.
 */
public class StoreOneLoginApiKeyOperation implements Operation<StoreOneLoginApiKeyCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String OLD_ONELOGIN_API_KEY_PATH = "shared/onelogin/api_key";

    public static final String NEW_ONELOGIN_API_KEY_PATH = "shared/onelogin/config";

    public static final String OLD_KEY = "value";

    public static final String NEW_KEY = "onelogin.api.key";

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public StoreOneLoginApiKeyOperation(final VaultAdminClientFactory vaultAdminClientFactory) {
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(final StoreOneLoginApiKeyCommand command) {
        logger.info("Getting client for Vault leader.");
        final Optional<VaultAdminClient> client = vaultAdminClientFactory.getClientForLeader();

        if (client.isPresent()) {
            Map<String, String> oldMap = Maps.newHashMap();
            oldMap.put(OLD_KEY, command.getApiKey());
            logger.info("Writing OneLogin API key at {}.", OLD_ONELOGIN_API_KEY_PATH);
            client.get().write(OLD_ONELOGIN_API_KEY_PATH, oldMap);

            Map<String, String> newMap = Maps.newHashMap();
            newMap.put(NEW_KEY, command.getApiKey());
            logger.info("Writing OneLogin API key at {}.", NEW_ONELOGIN_API_KEY_PATH);
            client.get().write(NEW_ONELOGIN_API_KEY_PATH, newMap);

            logger.info("OneLogin API key written.");
        } else {
            throw new IllegalStateException("Unable to determine Vault leader, aborting...");
        }
    }

    @Override
    public boolean isRunnable(final StoreOneLoginApiKeyCommand command) {
        return vaultAdminClientFactory.getClientForLeader().isPresent();
    }
}
