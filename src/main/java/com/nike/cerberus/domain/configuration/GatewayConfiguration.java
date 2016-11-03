package com.nike.cerberus.domain.configuration;

/**
 * Gateway configuration POJO.
 */
public class GatewayConfiguration {

    private String siteConfig;

    private String globalConfig;

    public String getSiteConfig() {
        return siteConfig;
    }

    public GatewayConfiguration setSiteConfig(String siteConfig) {
        this.siteConfig = siteConfig;
        return this;
    }

    public String getGlobalConfig() {
        return globalConfig;
    }

    public GatewayConfiguration setGlobalConfig(String globalConfig) {
        this.globalConfig = globalConfig;
        return this;
    }
}
