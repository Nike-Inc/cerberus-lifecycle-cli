package com.nike.cerberus.command.audit;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.EnableAuditLoggingOperation;

import static com.nike.cerberus.command.audit.EnableAuditLoggingCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Enables the CLI to set the required CMS properties to enable audit logging, when creating or updating CMS config"
)
public class EnableAuditLoggingCommand implements Command {

    public static final String COMMAND_NAME = "enable-audit-logging";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return EnableAuditLoggingOperation.class;
    }
}
