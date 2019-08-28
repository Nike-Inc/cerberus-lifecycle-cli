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

package com.nike.cerberus.operation.core;

import com.nike.cerberus.command.core.AddJwtSecretCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Operation for add/rotate JWT secrets.
 */
public class AddJwtSecretOperation implements Operation<AddJwtSecretCommand> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    @Inject
    public AddJwtSecretOperation(ConfigStore configStore) {

        this.configStore = configStore;
    }

    @Override
    public void run(AddJwtSecretCommand command) {
        long activationDelay = command.getActivationDelay();
        configStore.addJwtKey(activationDelay);

    }

    @Override
    public boolean isRunnable(AddJwtSecretCommand command) {
        return true;
    }
}
