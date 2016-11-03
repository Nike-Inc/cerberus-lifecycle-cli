package com.nike.cerberus.command.vault;

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

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsVaultTokenOperation.class;
    }
}
