package com.nike.cerberus.command.consul;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.consul.CreateConsulClusterOperation;

import static com.nike.cerberus.command.consul.CreateConsulClusterCommand.COMMAND_NAME;

/**
 * Command to create the consul cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the consul cluster.")
public class CreateConsulClusterCommand implements Command {

    public static final String COMMAND_NAME = "create-consul-cluster";

    @ParametersDelegate
    private StackDelegate stackDelegate = new StackDelegate();

    public StackDelegate getStackDelegate() {
        return stackDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateConsulClusterOperation.class;
    }
}
