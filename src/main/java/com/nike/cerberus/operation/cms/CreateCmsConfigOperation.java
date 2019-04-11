/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.CMS_ADMIN_GROUP_KEY;

/**
 * Gathers all of the CMS environment configuration and puts it in the config bucket.
 */
public class CreateCmsConfigOperation implements Operation<CreateCmsConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    @Inject
    public CreateCmsConfigOperation(final ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void run(final CreateCmsConfigCommand command) {
        logger.info("Retrieving configuration data from the configuration bucket.");
        final Properties cmsConfigProperties = configStore.getCmsSystemProperties(false, false);

        cmsConfigProperties.put(CMS_ADMIN_GROUP_KEY, command.getAdminGroup());

        command.getAdditionalProperties().forEach((k, v) -> {
            if (!cmsConfigProperties.containsKey(k)) {
                cmsConfigProperties.put(k, v);
            } else {
                logger.warn("Ignoring additional property that would override system configured property, " + k);
            }
        });

        logger.info("Uploading the CMS configuration to the configuration bucket.");
        configStore.storeCmsEnvConfig(cmsConfigProperties);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateCmsConfigCommand command) {
        boolean isRunnable = !configStore.getCmsEnvConfig().isPresent();

        if (!isRunnable) {
            logger.warn("CMS config already exists, use 'update-cms-config' command.");
        }

        return isRunnable;
    }
}
