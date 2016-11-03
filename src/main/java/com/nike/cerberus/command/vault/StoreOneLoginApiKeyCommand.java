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
