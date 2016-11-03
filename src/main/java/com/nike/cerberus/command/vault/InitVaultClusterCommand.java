package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.InitVaultClusterOperation;

import static com.nike.cerberus.command.vault.InitVaultClusterCommand.COMMAND_NAME;

/**
 * Command to initialize the Vault cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Initializes the Vault cluster.")
public class InitVaultClusterCommand implements Command {

    public static final String COMMAND_NAME = "init-vault-cluster";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return InitVaultClusterOperation.class;
    }
}
