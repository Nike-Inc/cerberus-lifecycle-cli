package com.nike.cerberus.command.consul;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.consul.CreateVaultAclOperation;

import static com.nike.cerberus.command.consul.CreateVaultAclCommand.COMMAND_NAME;

/**
 * Command to create the Vault ACL with Consul.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the Vault ACL with Consul.")
public class CreateVaultAclCommand implements Command {

    public static final String COMMAND_NAME = "create-vault-acl";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateVaultAclOperation.class;
    }
}
