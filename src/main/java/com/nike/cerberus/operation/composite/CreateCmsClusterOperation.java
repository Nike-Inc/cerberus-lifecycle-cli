/*
 * Copyright (c) 2019 Nike, Inc.
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

import com.beust.jcommander.internal.Lists;
import com.nike.cerberus.command.cms.CreateCmsAsgCommand;
import com.nike.cerberus.command.composite.CreateCmsClusterCommand;
import com.nike.cerberus.command.core.CreateInstanceProfileCommand;

import java.util.List;

public class CreateCmsClusterOperation extends CompositeOperation<CreateCmsClusterCommand> {

    @Override
    protected List<ChainableCommand> getCompositeCommandChain(CreateCmsClusterCommand compositeCommand) {
        return Lists.newArrayList(
                new ChainableCommand(new CreateInstanceProfileCommand()),
                ChainableCommand.Builder.create().withCommand(new CreateCmsAsgCommand()).withAdditionalArg(compositeCommand.getStackDelegate().getArgs()).build()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunnable(CreateCmsClusterCommand command) {
        // Always return true and depend on isRunnable of the chained commands to skip commands that cannot be re-ran
        // we want this command to be able to be run more than once to complete an environment for example say temporary
        // aws creds expire half way through environment creation
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnvironmentConfigRequired() {
        // We want this command to be able to run with or without an environment config provided
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean skipOnNotRunnable() {
        // Since we always return true for isRunnable on this command sub of the sub commands may have already been run
        // we want this command to be able to be run more than once to complete an environment for example say temporary
        // aws creds expire half way through environment creation
        return true;
    }
}
