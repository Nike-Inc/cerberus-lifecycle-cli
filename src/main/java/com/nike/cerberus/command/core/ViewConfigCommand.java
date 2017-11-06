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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static com.nike.cerberus.command.core.ViewConfigCommand.COMMAND_NAME;

/**
 * Command to view configuration files in S3.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Shows configuration files from S3.")
public class ViewConfigCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "view-config";

    @Parameter(names = {"--config-path"}, required = true, description = "The path to the configuration file (e.g. data/cms/environment.properties)")
    private String pathToConfig;

    private final ConfigStore configStore;

    @Inject
    public ViewConfigCommand(final ConfigStore configStore) {
        this.configStore = configStore;
    }

    @Override
    public void execute() {

        String path = pathToConfig;
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
            }
            else {
                logger.error(String.format("Failed to load file: '%s'", path));
            }
        }
    }

    @Override
    public boolean isRunnable() {
        return true;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
}
