package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.LoadDefaultVaultPoliciesOperation;

import static com.nike.cerberus.command.vault.LoadDefaultVaultPoliciesCommand.COMMAND_NAME;

/**
 * Command to initialize the Vault cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Loads the default policies into Vault.")
public class LoadDefaultVaultPoliciesCommand implements Command {

    public static final String COMMAND_NAME = "load-default-policies";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return LoadDefaultVaultPoliciesOperation.class;
    }
}
