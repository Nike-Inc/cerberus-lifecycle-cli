package com.nike.cerberus.operation.audit;

import com.nike.cerberus.command.audit.EnableAuditLoggingCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class EnableAuditLoggingOperation implements Operation<EnableAuditLoggingCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final String environmentName;

    @Inject
    public EnableAuditLoggingOperation(CloudFormationService cloudFormationService,
                                       ConfigStore configStore,
                                       @Named(ENV_NAME) String environmentName) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.environmentName = environmentName;
    }

    @Override
    public void run(EnableAuditLoggingCommand command) {
        configStore.setAuditLoggingEnabled(true);
        log.info("Audit Logging enabled for CMS, please make sure to run update-cms-config and reboot-cms");
    }

    @Override
    public boolean isRunnable(EnableAuditLoggingCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.AUDIT.getFullName(environmentName))) {
            log.info("The audit stack has not been created, please make sure to run create-audit-logging-stack and create-audit-log-athena-db-and-table before enabling the audit log in CMS");
            isRunnable = false;
        }

        return isRunnable;
    }
}
