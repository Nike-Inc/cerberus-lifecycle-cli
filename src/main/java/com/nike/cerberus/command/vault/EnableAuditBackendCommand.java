package com.nike.cerberus.command.vault;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.vault.AuditBackend;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.vault.EnableAuditBackendOperation;

import java.nio.file.Path;

import static com.nike.cerberus.command.vault.EnableAuditBackendCommand.COMMAND_NAME;

/**
 * Command to enable the audit backend for Vault.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Enables the audit backend for Vault.")
public class EnableAuditBackendCommand implements Command {

    public static final String COMMAND_NAME = "enable-audit-backend";

    @Parameter(names = {"--backend"}, required = true, description = "Audit backend.")
    private AuditBackend auditBackend;

    @Parameter(names = {"--file-path"},
            description = "File path for where the audit log will be written on the Vault instance. " +
                    "Only required for the file backend type.")
    private Path filePath;

    public AuditBackend getAuditBackend() {
        return auditBackend;
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return EnableAuditBackendOperation.class;
    }
}
