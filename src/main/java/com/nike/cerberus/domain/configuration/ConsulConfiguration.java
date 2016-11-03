package com.nike.cerberus.domain.configuration;

import com.nike.cerberus.domain.template.ConsulConfigurationInput;

/**
 * Consul configuration POJO.
 */
public class ConsulConfiguration {

    private ConsulConfigurationInput input;

    private String clientConfiguration;

    private String serverConfiguration;

    public ConsulConfigurationInput getInput() {
        return input;
    }

    public ConsulConfiguration setInput(ConsulConfigurationInput input) {
        this.input = input;
        return this;
    }

    public String getClientConfiguration() {
        return clientConfiguration;
    }

    public ConsulConfiguration setClientConfiguration(String clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        return this;
    }

    public String getServerConfiguration() {
        return serverConfiguration;
    }

    public ConsulConfiguration setServerConfiguration(String serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
        return this;
    }
}
