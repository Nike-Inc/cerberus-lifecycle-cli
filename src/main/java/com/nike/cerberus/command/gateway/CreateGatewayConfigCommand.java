package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.CreateGatewayConfigOperation;

import static com.nike.cerberus.command.gateway.CreateGatewayConfigCommand.COMMAND_NAME;

/**
 * Command to create the gateway configuration.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the gateway configuration for the cluster.")
public class CreateGatewayConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-gateway-config";

    @Parameter(names = "--hostname",
            description = "The hostname that will be used to expose this Cerberus environment.",
            required = true)
    private String hostname;

    public String getHostname() {
        return hostname;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateGatewayConfigOperation.class;
    }
}
