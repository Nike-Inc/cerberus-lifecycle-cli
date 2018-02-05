package com.nike.cerberus.command.audit;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.DisableAuditLoggingOperation;

import static com.nike.cerberus.command.audit.DisableAuditLoggingCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Disables the CLI to set the required CMS properties to enable audit logging, when creating or updating CMS config"
)
public class DisableAuditLoggingCommand implements Command {

    public static final String COMMAND_NAME = "disable-audit-logging";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return DisableAuditLoggingOperation.class;
    }
}
