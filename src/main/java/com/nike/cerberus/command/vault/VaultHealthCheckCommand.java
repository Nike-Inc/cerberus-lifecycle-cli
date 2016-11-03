package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.VaultHealthCheckOperation;

import static com.nike.cerberus.command.vault.UnsealVaultClusterCommand.COMMAND_NAME;

/**
 * Polls health statuses for vault cluster
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Reports the health of each Vault instance.")
public class VaultHealthCheckCommand implements Command {

    public static final String COMMAND_NAME = "vault-health";

    @Parameter(names = "--poll",
            description = "Flag for polling health check vs running once")
    private boolean poll;

    public boolean isPoll() {
        return poll;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return VaultHealthCheckOperation.class;
    }
}
