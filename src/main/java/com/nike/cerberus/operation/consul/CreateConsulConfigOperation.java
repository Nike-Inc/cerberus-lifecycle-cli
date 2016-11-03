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

        logger.info("Uploading Consul configuration to the configuration bucket.");
        configStore.storeConsulConfig(consulConfiguration);

        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateConsulConfigCommand command) {
        final boolean hasConsulConfig = configStore.hasConsulConfig();

        if (hasConsulConfig) {
            logger.error("Consul configuration present for specified environment, use the update command.");
        }

        return !hasConsulConfig;
    }
}
