package com.nike.cerberus.operation.composite;

import com.google.common.collect.ImmutableList;
import com.nike.cerberus.command.composite.DeleteEnvironmentCommand;
import com.nike.cerberus.command.core.DeleteStackCommand;
import com.nike.cerberus.service.ConsoleService;
import com.nike.cerberus.store.ConfigStore;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.LinkedList;
import java.util.List;

import static com.nike.cerberus.domain.environment.Stack.*;
import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class DeleteEnvironmentOperation extends CompositeOperation<DeleteEnvironmentCommand> {

    private final ConsoleService consoleService;

    private final String environmentName;

    private final ConfigStore configStore;

    @Inject
    public DeleteEnvironmentOperation(ConsoleService consoleService,
                                      @Named(ENV_NAME) String environmentName,
                                      ConfigStore configStore) {

        this.consoleService = consoleService;
        this.environmentName = environmentName;
        this.configStore = configStore;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain(DeleteEnvironmentCommand compositeCommand) {
        List<ChainableCommand> chainableCommandList = new LinkedList<>();

        // todo delete the kms keys generated by cms
        ImmutableList.of(
                ROUTE53,
                WAF,
                CMS,
                LOAD_BALANCER,
                DATABASE,
                SECURITY_GROUPS,
                VPC
        ).forEach(stack ->
                chainableCommandList.add(ChainableCommand.Builder.create()
                        .withCommand(new DeleteStackCommand())
                        .withOption(DeleteStackCommand.STACK_NAME_LONG_ARG, stack.getName())
                        .build())
        );

        configStore.getConfigEnabledRegions().forEach(region -> {
            chainableCommandList.add(ChainableCommand.Builder.create()
                    .withCommand(new DeleteStackCommand())
                    .withOption(DeleteStackCommand.STACK_NAME_LONG_ARG, REGION_BUCKET_AND_CMKS.getName())
                    .withOption(DeleteStackCommand.REGION_LONG_ARG, region.getName())
                    .build());
        });

        chainableCommandList.add(ChainableCommand.Builder.create()
                .withCommand(new DeleteStackCommand())
                .withAdditionalArg(DeleteStackCommand.STACK_NAME_LONG_ARG)
                .withAdditionalArg(CMS_IAM_ROLE.getName())
                .build());

        return chainableCommandList;
    }

    @Override
    public boolean isRunnable(DeleteEnvironmentCommand command) {
        try {
            String warning = String.format(
                    "This will delete the environment '%s' including all the secure data.",
                    environmentName
            );
            consoleService.askUserToProceed(warning,  ConsoleService.DefaultAction.NO);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isEnvironmentConfigRequired() {
        return false;
    }

    @Override
    public boolean skipOnNotRunnable() {
        return true;
    }
}
