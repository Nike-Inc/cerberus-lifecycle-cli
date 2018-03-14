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

package com.nike.cerberus.operation.core;

import com.nike.cerberus.command.core.ViewConfigCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

/**
 * Displays the given configuration file from S3.
 */
public class ViewConfigOperation implements Operation<ViewConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    @Inject
    public ViewConfigOperation(final ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void run(final ViewConfigCommand command) {

        String path = command.getPathToConfig();
        if (StringUtils.startsWith(path, "/")) {
            logger.warn("Path started with leading slash, removing...");
            path = StringUtils.stripStart(path, "/");
        }

        Optional<String> fileContents = configStore.getConfigProperties(path);
        if (fileContents.isPresent()) {
            logger.info(fileContents.get());
        } else {
            Set<String> keys = configStore.listUnderPartialPath(path);
            if (!keys.isEmpty()) {
                logger.info("List under path '{}': {}", path, keys);
            } else {
                logger.error(String.format("Failed to load file: '%s'", path));
            }
        }
    }

    @Override
    public boolean isRunnable(final ViewConfigCommand command) {
        configStore.isConfigSynchronized();
        return true;
    }
}
