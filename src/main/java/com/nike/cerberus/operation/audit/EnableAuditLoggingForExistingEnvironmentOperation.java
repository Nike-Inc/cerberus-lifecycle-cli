package com.nike.cerberus.operation.audit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand;
import com.nike.cerberus.command.audit.CreateAuditLoggingStackCommand;
import com.nike.cerberus.command.audit.EnableAuditLoggingCommand;
import com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.core.RebootCmsCommand;
import com.nike.cerberus.domain.cloudformation.ConfigParameters;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.composite.ChainableCommand;
import com.nike.cerberus.operation.composite.CompositeOperation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class EnableAuditLoggingForExistingEnvironmentOperation extends CompositeOperation<EnableAuditLoggingForExistingEnvironmentCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFormationService cloudFormationService;
    private final ConfigStore configStore;
    private final String environmentName;

    @Inject
    public EnableAuditLoggingForExistingEnvironmentOperation(CloudFormationService cloudFormationService,
                                                             ConfigStore configStore,
                                                             @Named(ENV_NAME) String environmentName) {

        this.cloudFormationService = cloudFormationService;
        this.configStore = configStore;
        this.environmentName = environmentName;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain(EnableAuditLoggingForExistingEnvironmentCommand compositeCommand) {
        String adminArn = configStore.getStackParameters(
                configStore.getPrimaryRegion(),
                Stack.CONFIG.getFullName(environmentName),
                ConfigParameters.class).getAccountAdminArn();

        return ImmutableList.of(
                ChainableCommand.Builder.create()
                        .withCommand(new CreateAuditLoggingStackCommand())
                        .withOption(CreateAuditLoggingStackCommand.ADMIN_ROLE_ARN_LONG_ARG, adminArn)
                        .build(),
                new ChainableCommand(new CreateAuditAthenaDbAndTableCommand()),
                new ChainableCommand(new EnableAuditLoggingCommand()),
                new ChainableCommand(new UpdateCmsConfigCommand()),
                new ChainableCommand(new RebootCmsCommand())
        );
    }

    @Override
    public boolean isRunnable(EnableAuditLoggingForExistingEnvironmentCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.CMS.getFullName(environmentName))) {
            log.info("The CMS stack does not exist, this command is intended to be ran on and environment that already exists");
            isRunnable = false;
        }

        return isRunnable;
    }

    @Override
    public boolean isEnvironmentConfigRequired() {
        return false;
    }
}
