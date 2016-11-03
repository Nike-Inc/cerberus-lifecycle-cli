package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.EnableConfigReplicationOperation;

import static com.nike.cerberus.command.core.EnableConfigReplicationCommand.COMMAND_NAME;

/**
 * Command for enabling replication of the config bucket.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Enables replication of the config bucket.")
public class EnableConfigReplicationCommand implements Command {

    public static final String COMMAND_NAME = "enable-config-replication";

    @Parameter(names = "--replication-region",
            description = "The region to create the replication bucket in.",
            required = true)
    private String replicationRegion;

    public String getReplicationRegion() {
        return replicationRegion;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return EnableConfigReplicationOperation.class;
    }
}
