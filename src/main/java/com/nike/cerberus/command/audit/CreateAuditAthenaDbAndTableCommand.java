package com.nike.cerberus.command.audit;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.audit.CreateAuditAthenaDbAndTableOperation;

import static com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand.COMMAND_NAME;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Creates the db and table needed in athena to enable interacting with the audit data via athena"
)
public class CreateAuditAthenaDbAndTableCommand implements Command {

    public static final String COMMAND_NAME = "create-audit-log-athena-db-and-table";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateAuditAthenaDbAndTableOperation.class;
    }
}
