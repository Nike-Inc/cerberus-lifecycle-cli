package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.UnsealVaultClusterOperation;

import static com.nike.cerberus.command.vault.UnsealVaultClusterCommand.COMMAND_NAME;

/**
 * Command to unseal the Vault cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Unseals the Vault cluster.")
public class UnsealVaultClusterCommand implements Command {

    public static final String COMMAND_NAME = "unseal-vault-cluster";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UnsealVaultClusterOperation.class;
    }
}
