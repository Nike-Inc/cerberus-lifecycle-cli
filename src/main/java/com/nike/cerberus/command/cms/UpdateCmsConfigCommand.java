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

package com.nike.cerberus.command.cms;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.CMS_ADMIN_GROUP_KEY;
import static com.nike.cerberus.ConfigConstants.SYSTEM_CONFIGURED_CMS_PROPERTIES;
import static com.nike.cerberus.command.cms.UpdateCmsConfigCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the CMS config.")
public class UpdateCmsConfigCommand implements Command {

    public static final String COMMAND_NAME = "update-cms-config";

    public static final String OVERWRITE_LONG_ARG = "--overwrite";

    @Parameter(names = CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG, description = "Group that has admin privileges in CMS.")
    private String adminGroup;

    @Parameter(names = OVERWRITE_LONG_ARG, description = "Overwrite option deletes any existing -P parameters that were not resupplied (this option is not usually needed).")
    private boolean overwrite;

    @DynamicParameter(names = CreateCmsConfigCommand.PROPERTY_SHORT_ARG, description = "Dynamic parameters for setting additional properties in the CMS environment configuration.")
    private Map<String, String> additionalProperties = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    @Inject
    public UpdateCmsConfigCommand(final ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void execute() {

        logger.debug("Retrieving configuration data from the configuration bucket.");

        final Properties newProperties = configStore.getCmsSystemProperties();
        final Properties existingCustomProperties = configStore.getExistingCmsUserProperties();
        if (! overwrite) {
            // keep existing custom properties
            newProperties.putAll(existingCustomProperties);
        }

        // update existing custom properties, add new ones
        additionalProperties.forEach((k, v) -> {
            if (! SYSTEM_CONFIGURED_CMS_PROPERTIES.contains(k)) {
                newProperties.put(k, v);
            } else {
                logger.warn("Ignoring additional property that would override system configured property, " + k);
            }
        });

        final String existingAdminGroup = existingCustomProperties.getProperty(CMS_ADMIN_GROUP_KEY);
        final String adminGroupParameter = adminGroup;
        String newAdminGroupValue = existingAdminGroup;  // keep existing admin group by default

        if (shouldOverwriteAdminGroup(existingAdminGroup, adminGroupParameter)) {
            logger.warn(String.format("Updating CMS admin group from '%s' to '%s'", existingAdminGroup, adminGroupParameter));
            configStore.storeCmsAdminGroup(adminGroupParameter);
            newAdminGroupValue = adminGroupParameter;  // overwrite admin group
        }
        newProperties.put(CMS_ADMIN_GROUP_KEY, newAdminGroupValue);

        logger.info("Uploading the CMS configuration to the configuration bucket.");
        configStore.storeCmsEnvConfig(newProperties);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable() {
        boolean isRunnable = true;
        final Optional<String> cmsDatabasePassword = configStore.getCmsDatabasePassword();

        if (!cmsDatabasePassword.isPresent()) {
            logger.error("CMS database password not present for specified environment.");
            isRunnable = false;
        }

        return isRunnable;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
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
