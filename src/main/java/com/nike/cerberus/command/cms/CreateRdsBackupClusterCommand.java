package com.nike.cerberus.command.cms;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.CreateRdsBackupClusterOperation;

import static com.nike.cerberus.command.cms.CreateRdsBackupClusterCommand.COMMAND_NAME;

/**
 * Command to create the RDS backup cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the RDS backup cluster.")
public class CreateRdsBackupClusterCommand implements Command {

    public static final String COMMAND_NAME = "create-rds-backup-cluster";

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
        return CreateRdsBackupClusterOperation.class;
    }
}
