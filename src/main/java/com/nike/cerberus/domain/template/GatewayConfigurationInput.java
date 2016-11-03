package com.nike.cerberus.domain.template;

/**
 * POJO for gateway configuration inputs.
 */
public class GatewayConfigurationInput {

    private String gatewayHost;

    private String dashboardHost;

    private String vaultHost;

    private String cmsHost;

    public String getGatewayHost() {
        return gatewayHost;
    }

    public GatewayConfigurationInput setGatewayHost(String gatewayHost) {
        this.gatewayHost = gatewayHost;
        return this;
    }

    public String getDashboardHost() {
        return dashboardHost;
    }

    public GatewayConfigurationInput setDashboardHost(String dashboardHost) {
        this.dashboardHost = dashboardHost;
        return this;
    }

    public String getVaultHost() {
        return vaultHost;
    }

    public GatewayConfigurationInput setVaultHost(String vaultHost) {
        this.vaultHost = vaultHost;
        return this;
    }

    public String getCmsHost() {
        return cmsHost;
    }

    public GatewayConfigurationInput setCmsHost(String cmsHost) {
        this.cmsHost = cmsHost;
        return this;
    }
}
