/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.operation.composite;

import com.beust.jcommander.JCommander;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.nike.cerberus.cli.EnvironmentConfigToArgsMapper;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.operation.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * This abstract class can be extended by any composite operation to enable chaining of commands
 * together to perform complex operations.
 *
 * @param <T> The command that this operation implements
 */
public abstract class CompositeOperation<T extends Command> implements Operation<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Injector injector;

    private EnvironmentConfig environmentConfig;

    private List<ChainableCommand> runnableChainedCommands = new LinkedList<>();

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Inject
    public void setEnvironmentConfig(@Nullable EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    private Operation getOperationInstance(Command command) {
        return injector.getInstance(command.getOperationClass());
    }

    /**
     * {@inheritDoc}
     *
     * Runs all the chained commands in order that are defined in the runnable chained command list,
     * which gets populated when isRunnable is executed
     */
    @SuppressWarnings("unchecked")
    public void run(T compositeCommand) {
        if (isEnvironmentConfigRequired() && environmentConfig == null) {
            throw new RuntimeException(String.format("The %s command requires that -f or --file must be supplied as a global option with " +
                    "a path to a valid environment yaml", compositeCommand.getCommandName()));
        }

        for (ChainableCommand chainableCommand : getCompositeCommandChain()) {
            Command chainedCommand = chainableCommand.getCommand();
            String[] additionalArgs = chainableCommand.getAdditionalArgs();

            // Parse the yaml and additional args to get args to pass to jcommander
            List<String> argsList = EnvironmentConfigToArgsMapper.getArgsForCommand(
                    environmentConfig, chainedCommand.getCommandName(), additionalArgs);

            // If the mapper doesn't have a mapping for a given command it will return an empty list
            // in this case we will just use the args manually provided by the chainable command
            String[] args;
            if (argsList.size() > 0) {
                args = argsList.toArray(new String[argsList.size()]);
            } else {
                args = additionalArgs;
            }

            // Use jcommander to bind the resolved args to the command object
            JCommander.newBuilder().addObject(chainedCommand).build().parse(args);

            // Get the instance of the operation from guice
            Operation operation = getOperationInstance(chainedCommand);

            // If the given command is not runnable return false for the whole chain
            //noinspection unchecked
            boolean isRunnable = operation.isRunnable(chainedCommand);
            if (! isRunnable) {
                if (! skipOnNotRunnable()) {
                    throw new RuntimeException("The command: " + chainedCommand.getCommandName() + " is not runnable, breaking the chain");
                } else {
                    log.info("The command {} reports that it is not runnable, skipping...", chainedCommand.getCommandName());
                    continue;
                }
            }

            log.info("Attempting to run command: {}, with args: {}", chainedCommand.getCommandName(), args);
            try {
                operation.run(chainedCommand);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to execute chained command: " + chainedCommand.getCommandName(), e);
            }
            log.info("Finished command: {}\n", chainedCommand.getCommandName());
        }
    }

    /**
     * Implement this method to define the ordered list of chained commands that will get executed
     *
     * @return An ordered list of ChainableCommand's
     */
    protected abstract List<ChainableCommand> getCompositeCommandChain();

    /**
     * If you command doesn't require that the environment yaml be supplied, you can override this to false.
     *
     * @return boolean of whether or not the environment yaml is required.
     */
    public boolean isEnvironmentConfigRequired() {
        return true;
    }

    /**
     * If your chain of commands doesn't need every command to run to succeed you can override this to tru.
     *
     * For example you could have a chain of commands that is long and complicated and commands that have completed will
     * return isRunnable: false on future runs, setting this to return true would just mean that the command that is
     * already run will be skipped moving on to commands that are still needing to be ran. Allowing you to run a
     * complicated composite command as many times as needed to succeed
     *
     * @return boolean of whether or not to fail the command if a command in the chain returns false for isRunnable
     */
    public boolean skipOnNotRunnable() {
        return false;
    }
}
