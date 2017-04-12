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

package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.CreateCmsVaultTokenOperation;

import static com.nike.cerberus.command.vault.CreateCmsVaultTokenCommand.COMMAND_NAME;

/**
 * Command to create the Vault token for CMS.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the Vault token for CMS.")
public class CreateCmsVaultTokenCommand implements Command {

    public static final String COMMAND_NAME = "create-cms-vault-token";

    public static final String FORCE_OVERWRITE_LONG_ARG = "--force-overwrite";

    // This option is mainly useful if:
    // 1) you want to go through some manual steps to rotate the CMS vault token
    // 2) you've partially hosed a development system
    @Parameter(names = FORCE_OVERWRITE_LONG_ARG,
            description = "Force overwriting existing CMS vault token in secrets.json.  It is important to manually revoke the old CMS token when using this option.")
    private boolean forceOverwrite = false;

    public boolean isForceOverwrite() {
        return forceOverwrite;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsVaultTokenOperation.class;
    }
}
