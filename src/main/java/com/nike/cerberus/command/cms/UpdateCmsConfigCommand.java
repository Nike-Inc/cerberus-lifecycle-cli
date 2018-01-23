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
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.UpdateCmsConfigOperation;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.cms.UpdateCmsConfigCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the CMS config.")
public class UpdateCmsConfigCommand implements Command {

    public static final String COMMAND_NAME = "update-cms-config";
    public static final String OVERWRITE_LONG_ARG = "--overwrite";
    public static final String FORCE_ARG = "--force";

    @Parameter(names = CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG, description = "Group that has admin privileges in CMS.")
    private String adminGroup;

    @Parameter(names = OVERWRITE_LONG_ARG, description = "Overwrite option deletes any existing -P parameters that were not resupplied (this option is not usually needed).")
    private boolean overwrite;

    @DynamicParameter(names = CreateCmsConfigCommand.PROPERTY_SHORT_ARG, description = "Dynamic parameters for setting additional properties in the CMS environment configuration.")
    private Map<String, String> additionalProperties = new HashMap<>();

    @Parameter(names = FORCE_ARG, description = "Force allow overwriting of system generated property. This may break your configuration.")
    private boolean force = false;

    public String getAdminGroup() {
        return adminGroup;
    }

    public boolean getOverwrite() {
        return overwrite;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateCmsConfigOperation.class;
    }
}
