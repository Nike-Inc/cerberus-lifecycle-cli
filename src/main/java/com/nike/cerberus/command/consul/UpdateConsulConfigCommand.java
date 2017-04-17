package com.nike.cerberus.command.consul;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.consul.UpdateConsulConfigOperation;

import static com.nike.cerberus.command.consul.CreateConsulConfigCommand.COMMAND_NAME;

@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the consul configuration for the cluster while maintaining the secrets in the current config.")
public class UpdateConsulConfigCommand implements Command {

    public static final String COMMAND_NAME = "update-consul-config";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateConsulConfigOperation.class;
    }
}