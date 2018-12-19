/*
 * Copyright (c) 2017 Nike, Inc.
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

package com.nike.cerberus.operation.cms;

import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.CMS_ADMIN_GROUP_KEY;
import static com.nike.cerberus.ConfigConstants.SYSTEM_CONFIGURED_CMS_PROPERTIES;

/**
 * Gathers all of the CMS environment configuration and puts it in the config bucket.
 */
public class UpdateCmsConfigOperation implements Operation<UpdateCmsConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    @Inject
    public UpdateCmsConfigOperation(final ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void run(final UpdateCmsConfigCommand command) {

        logger.debug("Retrieving configuration data from the configuration bucket.");

        final Properties newProperties = configStore.getCmsSystemProperties(true, command.isIgnoreDefaultConfigurations());
        final Properties existingCustomProperties = configStore.getExistingCmsUserProperties();
        if (!command.getOverwrite()) {
            // keep existing custom properties
            newProperties.putAll(existingCustomProperties);
        }

        // update existing custom properties, add new ones
        command.getAdditionalProperties().forEach((k, v) -> {
            if (!SYSTEM_CONFIGURED_CMS_PROPERTIES.contains(k) || command.isForce()) {
                newProperties.put(k, v);
            } else {
                logger.warn("Ignoring additional property that would override system configured property, " + k);
            }
        });

        final String existingAdminGroup = existingCustomProperties.getProperty(CMS_ADMIN_GROUP_KEY);
        final String adminGroupParameter = command.getAdminGroup();
        String newAdminGroupValue = existingAdminGroup;  // keep existing admin group by default

        if (shouldOverwriteAdminGroup(existingAdminGroup, adminGroupParameter)) {
            logger.warn(String.format("Updating CMS admin group from '%s' to '%s'", existingAdminGroup, adminGroupParameter));
            newAdminGroupValue = adminGroupParameter;  // overwrite admin group
        }
        newProperties.put(CMS_ADMIN_GROUP_KEY, newAdminGroupValue);

        logger.info("Uploading the CMS configuration to the configuration bucket.");
        configStore.storeCmsEnvConfig(newProperties);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final UpdateCmsConfigCommand command) {
        boolean isRunnable = configStore.getCmsEnvConfig().isPresent();
        final Optional<String> cmsDatabasePassword = configStore.getCmsDatabasePassword();

        if (!cmsDatabasePassword.isPresent()) {
            logger.error("CMS database password not present for specified environment.");
            isRunnable = false;
        }

        return isRunnable;
    }

    private boolean shouldOverwriteAdminGroup(final String existingAdminGroup, final String newAdminGroup) {
        if (newAdminGroup == null && existingAdminGroup == null) {
            throw new IllegalStateException("Admin group does not exist in S3 config and was not provided as a " +
                    "parameter. Please use --admin-group parameter to fix.");
        }

        if (newAdminGroup == null) {
            logger.warn("Admin group not provided, using existing group.");
            return false;
        }

        return !StringUtils.equals(newAdminGroup, existingAdminGroup);
    }
}
