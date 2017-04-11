/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.operation.consul;

import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.consul.CreateConsulConfigCommand;
import com.nike.cerberus.domain.configuration.ConsulConfiguration;
import com.nike.cerberus.generator.ConsulConfigGenerator;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Generates the configuration files for Consul and uploads them to the config store.
 */
public class CreateConsulConfigOperation implements Operation<CreateConsulConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConsulConfigGenerator consulConfigGenerator;

    private final ConfigStore configStore;

    @Inject
    public CreateConsulConfigOperation(final ConsulConfigGenerator consulConfigGenerator,
                                       final ConfigStore configStore) {
        this.consulConfigGenerator = consulConfigGenerator;
        this.configStore = configStore;
    }

    @Override
    public void run(final CreateConsulConfigCommand command) {
        logger.info("Generating the Consul configuration.");
        final ConsulConfiguration consulConfiguration =
                consulConfigGenerator.generate(ConfigConstants.CONSUL_DATACENTER);

        if (configStore.hasConsulConfig()) {
            logger.info("Consul configuration was already present for specified environment, overwriting...");
        }

        logger.info("Uploading Consul configuration to the configuration bucket.");
        configStore.storeConsulConfig(consulConfiguration);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateConsulConfigCommand command) {
        return true;
    }
}
