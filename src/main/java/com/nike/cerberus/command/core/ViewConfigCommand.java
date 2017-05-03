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
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.ViewConfigOperation;

import static com.nike.cerberus.command.cms.CreateCmsClusterCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Shows the CMS config.")
public class ViewConfigCommand implements Command {

    public static final String COMMAND_NAME = "view-config";

    @Parameter(names = {"--config-path"}, required = true, description = "The path to the configuration file (e.g. data/cms/environment.properties)")
    private String pathToConfig;

    public String getPathToConfig() {
        return pathToConfig;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return ViewConfigOperation.class;
    }
}
