package com.nike.cerberus.domain.configuration;

/**
 * Represents the generated Vault configuration.
 */
public class VaultConfiguration {
    private String config;

    public VaultConfiguration setConfig(String config) {
        this.config = config;
        return this;
    }

    public String getConfig() {
        return config;
    }
}
