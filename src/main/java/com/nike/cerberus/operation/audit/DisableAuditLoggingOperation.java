package com.nike.cerberus.operation.audit;

import com.nike.cerberus.command.audit.DisableAuditLoggingCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisableAuditLoggingOperation implements Operation<DisableAuditLoggingCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    public DisableAuditLoggingOperation(ConfigStore configStore) {

        this.configStore = configStore;
    }

    @Override
    public void run(DisableAuditLoggingCommand command) {
        configStore.setAuditLoggingEnabled(false);
        log.info("Audit Logging disabled for CMS, please make sure to run update-cms-config and reboot-cms");
    }

    @Override
    public boolean isRunnable(DisableAuditLoggingCommand command) {
        boolean isRunnable = true;

        if (configStore.isAuditLoggingEnabled()) {
            log.info("Audit logging is not enabled, nothing to do");
            isRunnable = false;
        }

        return isRunnable;
    }
}
