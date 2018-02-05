package com.nike.cerberus.command.audit;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.EnableAuditLoggingForExistingEnvironmentOperation;

import static com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand.COMMAND_DESCRIPTION;
import static com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = COMMAND_DESCRIPTION
)
public class EnableAuditLoggingForExistingEnvironmentCommand implements Command {

    public static final String COMMAND_NAME = "enable-audit-logging-for-existing-environment";
    public static final String COMMAND_DESCRIPTION =
            "A Composite command that will will execute the following commands in order: "
                    + "create-audit-logging-stack, "
                    + "create-audit-log-athena-db-and-table, "
                    + "enable-audit-logging, "
                    + "update-cms-config, "
                    + "reboot-cms. "
                    + "This will do everything required to enable audit logging for an existing environment.";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return EnableAuditLoggingForExistingEnvironmentOperation.class;
    }
}
