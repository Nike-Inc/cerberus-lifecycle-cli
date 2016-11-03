package com.nike.cerberus.operation.vault;

import com.google.common.collect.Maps;
import com.nike.cerberus.command.vault.EnableAuditBackendCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.vault.VaultAdminClientFactory;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultEnableAuditBackendRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * Operation for enabling the audit backend for Vault.
 */
public class EnableAuditBackendOperation implements Operation<EnableAuditBackendCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaultAdminClientFactory vaultAdminClientFactory;

    @Inject
    public EnableAuditBackendOperation(final VaultAdminClientFactory vaultAdminClientFactory) {
        this.vaultAdminClientFactory = vaultAdminClientFactory;
    }

    @Override
    public void run(final EnableAuditBackendCommand command) {
        logger.info("Getting client for Vault leader.");
        final Optional<VaultAdminClient> client = vaultAdminClientFactory.getClientForLeader();

        if (client.isPresent()) {
            Map<String, String> options = Maps.newHashMap();

            switch (command.getAuditBackend()) {
                case FILE:
                    options = getFileOptions(command.getFilePath().toString());
                    break;
                case SYSLOG:
                    options = getSyslogOptions();
                    break;
            }

            final VaultEnableAuditBackendRequest request = new VaultEnableAuditBackendRequest()
                    .setType(command.getAuditBackend().getType())
                    .setDescription("Cerberus configured audit backend.")
                    .setOptions(options);

            logger.info("Enabling the audit backend in Vault.");
            client.get().enableAuditBackend(command.getAuditBackend().getType(), request);
            logger.info("Audit enabled.");
        } else {
            throw new IllegalStateException("Unable to determine Vault leader, aborting...");
        }
    }

    @Override
    public boolean isRunnable(final EnableAuditBackendCommand command) {
        return vaultAdminClientFactory.getClientForLeader().isPresent();
    }

    private Map<String, String> getFileOptions(final String filePath) {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("File path is required for the file audit backend type.");
        }

        final Map<String, String> options = Maps.newHashMap();
        options.put("file_path", filePath);
        options.put("log_raw", "false");
        options.put("hmac_accessor", "true");
        return options;
    }

    private Map<String, String> getSyslogOptions() {
        final Map<String, String> options = Maps.newHashMap();
        options.put("facility", "AUTH");
        options.put("tag", "vault");
        options.put("log_raw", "false");
        options.put("hmac_accessor", "true");
        return options;
    }
}
