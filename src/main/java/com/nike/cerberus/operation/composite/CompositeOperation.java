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

            // convert args list to args array
            String[] args = argsList.toArray(new String[argsList.size()]);

            // Bind args to object
            JCommander.newBuilder().addObject(chainedCommand).build().parse(args);

            // Get the instance of the operation from guice
            Operation operation = getOperationInstance(chainedCommand);

            //noinspection unchecked
            if (! operation.isRunnable(chainedCommand)) {
                return false;
            }
            log.debug("Command: {} with Args: {} is runnable, adding to runnable list", chainedCommand.getCommandName(), args);
            chainableCommand.setOperation(operation);
            runnableChainedCommands.add(chainableCommand);
        }
        return true;
    }

    protected abstract List<ChainableCommand> getCompositeCommandChain();

    public boolean getIsEnvironmentConfigRequired() {
        return true;
    }
}
