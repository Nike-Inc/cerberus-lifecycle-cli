package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.PrintStackInfoOperation;

import static com.nike.cerberus.command.core.PrintStackInfoCommand.COMMAND_NAME;

/**
 * Command for printing information about the specified CloudFormation stack.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Prints information about the CloudFormation stack.")
public class PrintStackInfoCommand implements Command {

    public static final String COMMAND_NAME = "print-stack-info";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to print information about.")
    private StackName stackName;

    public StackName getStackName() {
        return stackName;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return PrintStackInfoOperation.class;
    }
}
