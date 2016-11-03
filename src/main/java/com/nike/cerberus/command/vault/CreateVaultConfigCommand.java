package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.CreateVaultConfigOperation;

import static com.nike.cerberus.command.vault.CreateVaultConfigCommand.COMMAND_NAME;

/**
 * Command to create the vault configuration.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the vault configuration for the cluster.")
public class CreateVaultConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-vault-config";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateVaultConfigOperation.class;
    }
}
