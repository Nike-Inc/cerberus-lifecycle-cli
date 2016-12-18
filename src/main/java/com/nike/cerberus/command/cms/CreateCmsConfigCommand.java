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

package com.nike.cerberus.command.cms;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.CreateCmsConfigOperation;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.cms.CreateCmsClusterCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the CMS config.")
public class CreateCmsConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-cms-config";
    public static final String ADMIN_GROUP_LONG_ARG = "--admin-group";
    public static final String PROPERTY_SHORT_ARG = "-P";

    @Parameter(names = ADMIN_GROUP_LONG_ARG, description = "Group that has admin privileges in CMS.", required = true)
    private String adminGroup;

    @DynamicParameter(names = PROPERTY_SHORT_ARG, description = "Dynamic parameters for setting additional properties in the CMS environment configuration.")
    private Map<String, String> additionalProperties = new HashMap<>();

    public String getAdminGroup() {
        return adminGroup;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsConfigOperation.class;
    }
}
