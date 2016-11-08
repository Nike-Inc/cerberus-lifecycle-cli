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
import com.nike.cerberus.operation.vault.StoreOneLoginApiKeyOperation;

import static com.nike.cerberus.command.vault.StoreOneLoginApiKeyCommand.COMMAND_NAME;

/**
 * Command for storing the OneLogin API key which is required by CMS to operate.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Store the OneLogin API Key in Vault.")
public class StoreOneLoginApiKeyCommand implements Command {

    public static final String COMMAND_NAME = "store-onelogin-api-key";

    @Parameter(names = {"--api-key"}, password = true, required = true, description = "OneLogin API key")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return StoreOneLoginApiKeyOperation.class;
    }
}
