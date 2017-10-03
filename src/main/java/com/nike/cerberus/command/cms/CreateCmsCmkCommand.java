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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.CreateCmsCmkOperation;

import java.util.List;

import static com.nike.cerberus.command.cms.UpdateCmsConfigCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the CMS config.")
public class CreateCmsCmkCommand implements Command {

    public static final String COMMAND_NAME = "create-cms-cmk";
    public static final String ADDITIONAL_REGIONS_ARG = "--additional-regions";
    public static final String ROTATE_ARG = "--rotate";

    @Parameter(names = ADDITIONAL_REGIONS_ARG,
            description = "Additional regions to create the CMS CMK in. At least one additional region, other than the primary region where Cerberus will be running, is required.",
            variableArity = true,
            required = true
    )
    private List<String> additionalRegions;

    @Parameter(names = ROTATE_ARG,
            description = "Manually rotates the CMKs while leaving the old CMKs in place so they can still be used. " +
                    "This is generally unnecessary since AWS will automatically rotate keys previously created with this command. "
    )
    private boolean rotate = false;

    public List<String> getAdditionalRegions() {
        return additionalRegions;
    }

    public boolean isRotate() {
        return rotate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsCmkOperation.class;
    }
}
