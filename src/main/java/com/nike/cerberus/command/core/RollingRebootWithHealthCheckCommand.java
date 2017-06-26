package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.RollingRebootWithHealthCheckOperation;

import static com.nike.cerberus.command.core.RollingRebootWithHealthCheckCommand.COMMAND_NAME;

/**
 * Command to reboot the CMS cluster.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Performs a safe rolling reboot on instances in the given cluster, checking that " +
                "the previous instance is healthy before rebooting the next one."
)
public class RollingRebootWithHealthCheckCommand implements Command {

    public static final String COMMAND_NAME = "rolling-reboot";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to reboot.")
    private StackName stackName = StackName.CMS;

    public StackName getStackName() {
        return stackName;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return RollingRebootWithHealthCheckOperation.class;
    }
}
