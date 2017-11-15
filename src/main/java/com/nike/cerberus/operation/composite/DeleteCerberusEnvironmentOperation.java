package com.nike.cerberus.operation.composite;

import com.google.common.collect.ImmutableList;
import com.nike.cerberus.command.composite.DeleteCerberusEnvironmentCommand;
import com.nike.cerberus.command.core.DeleteStackCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.service.ConsoleService;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

import static com.nike.cerberus.domain.environment.Stack.*;

public class DeleteCerberusEnvironmentOperation extends CompositeOperation<DeleteCerberusEnvironmentCommand> {

    private final ConsoleService consoleService;

    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public DeleteCerberusEnvironmentOperation(ConsoleService consoleService,
                                              EnvironmentMetadata environmentMetadata) {

        this.consoleService = consoleService;
        this.environmentMetadata = environmentMetadata;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain() {
        List<ChainableCommand> chainableCommandList = new LinkedList<>();

        ImmutableList.of(
                ROUTE53,
                WAF,
                CMS,
                LOAD_BALANCER,
                DATABASE,
                SECURITY_GROUPS,
                VPC,
                BASE
        ).forEach(stack ->
                chainableCommandList.add(ChainableCommand.Builder.create()
                        .withCommand(new DeleteStackCommand())
                        .withAdditionalArg(DeleteStackCommand.STACK_NAME_LONG_ARG)
                        .withAdditionalArg(stack.getName())
                        .build())
        );

        return chainableCommandList;
    }

    @Override
    public boolean isRunnable(DeleteCerberusEnvironmentCommand command) {
        try {
            String warning = String.format(
                    "This will delete the environment '%s' including all the secure data.",
                    environmentMetadata.getName()
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
