package com.nike.cerberus.command.composite;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.composite.DeleteEnvironmentOperation;

import static com.nike.cerberus.command.composite.DeleteEnvironmentCommand.COMMAND_NAME;

@Parameters(
        commandNames = {
                COMMAND_NAME
        },
        commandDescription = "Deletes all the cf stacks, and data associated with a Cerberus environment that is managed by the CLI"
)
public class DeleteEnvironmentCommand implements Command {

    public static final String COMMAND_NAME = "delete-environment";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return DeleteEnvironmentOperation.class;
    }
}
