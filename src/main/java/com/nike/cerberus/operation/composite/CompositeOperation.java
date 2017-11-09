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
        runnableChainedCommands.forEach(chainableCommand -> {
            Command chainedCommand = chainableCommand.getCommand();
            Operation chainedOperation = chainableCommand.getOperation();
            log.info("Attempting to run command: {}", chainedCommand.getCommandName());
            try {
                chainedOperation.run(chainedCommand);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to execute chained command: " + chainedCommand.getCommandName(), e);
            }
            log.info("Finished command: {}", chainedCommand.getCommandName());

        });
    }

    /**
     * {@inheritDoc}
     *
     * Gets the command chain from the implementing class and iterates over every chained command operation to make sure
     * they are runnable, if any operation is not runnable this will return false
     */
    public boolean isRunnable(T command) {
        if (getIsEnvironmentConfigRequired() && environmentConfig == null) {
            throw new RuntimeException(String.format("The %s command requires that -f or --file must be supplied as a global option with " +
                    "a path to a valid environment yaml", command.getCommandName()));
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
            if (! operation.isRunnable(chainedCommand)) {
                return false;
            }

            // If the given command is runnable add the guice created operation to the object and add the command to the runnable list
            log.debug("Command: {} with Args: {} is runnable, adding to runnable list", chainedCommand.getCommandName(), args);
            chainableCommand.setOperation(operation);
            runnableChainedCommands.add(chainableCommand);
        }
        return true;
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
    public boolean getIsEnvironmentConfigRequired() {
        return true;
    }
}
