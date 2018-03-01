/*
 * Copyright (c) 2018 Nike, Inc.
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

import com.nike.cerberus.command.core.UpdateAllStackTagsCommand;
import com.nike.cerberus.command.core.UpdateStackTagsCommand;
import com.nike.cerberus.domain.environment.Stack;

import java.util.LinkedList;
import java.util.List;

/**
 * Operation class for UpdateAllStackTagsCommand
 */
public class UpdateAllStackTagsOperation extends CompositeOperation<UpdateAllStackTagsCommand> {

    /**
     * {@inheritDoc}
     * @param compositeCommand
     */
    @Override
    protected List<ChainableCommand> getCompositeCommandChain(UpdateAllStackTagsCommand compositeCommand) {
        List<ChainableCommand> commandList = new LinkedList<>();

        for (Stack stack : Stack.ALL_STACKS) {
            commandList.add(
                    ChainableCommand.Builder.create()
                            .withCommand(new UpdateStackTagsCommand())
                            .withAdditionalArg(UpdateStackTagsCommand.STACK_NAME_LONG_ARG)
                            .withAdditionalArg(stack.toString())
                            .withAdditionalArg(compositeCommand.getTagsDelegate().getArgs())
                            .build()
            );
        }

        return commandList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunnable(UpdateAllStackTagsCommand command) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnvironmentConfigRequired() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean skipOnNotRunnable() {
        return true;
    }
}
