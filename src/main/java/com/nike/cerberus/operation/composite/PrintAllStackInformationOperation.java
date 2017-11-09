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

import com.nike.cerberus.command.composite.PrintAllStackInformationCommand;
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.domain.environment.StackName;

import java.util.LinkedList;
import java.util.List;

/**
 * Composite Command for printing stack info for entire Cerberus Environment
 */
public class PrintAllStackInformationOperation extends CompositeOperation<PrintAllStackInformationCommand> {

    @Override
    protected List<ChainableCommand> getCompositeCommandChain() {
        List<ChainableCommand> commandList = new LinkedList<>();

        for (StackName stackName : StackName.values()) {
            commandList.add(
                    ChainableCommand.Builder.create()
                            .withCommand(new PrintStackInfoCommand())
                            .withAdditionalArg(PrintStackInfoCommand.STACK_NAME_LONG_ARG)
                            .withAdditionalArg(stackName.toString())
                            .build()
            );
        }

        return commandList;
    }

    @Override
    public boolean getIsEnvironmentConfigRequired() {
        return false;
    }

}
