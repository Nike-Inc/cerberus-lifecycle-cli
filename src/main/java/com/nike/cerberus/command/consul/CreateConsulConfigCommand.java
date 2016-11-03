package com.nike.cerberus.command.consul;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.consul.CreateConsulConfigOperation;

import static com.nike.cerberus.command.consul.CreateConsulConfigCommand.COMMAND_NAME;

/**
 * Command to create the consul configuration.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the consul configuration for the cluster.")
public class CreateConsulConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-consul-config";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateConsulConfigOperation.class;
    }
}

