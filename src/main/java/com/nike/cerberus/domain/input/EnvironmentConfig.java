package com.nike.cerberus.domain.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentConfig {

    private String version;
    private ProxyConfig proxyConfig;
    private String environmentName;
    private String region;
    private String costCenter;
    private String ownerEmail;
    private String ownerGroup;
    private String adminRoleArn;
    private String vpcHostedZoneName;
    private String hostname;
    private String hostedZoneId;
    private VpcAccessWhitelist vpcAccessWhitelist;
    private Consul consul;
    private Vault vault;
    private ManagementService managementService;
    private Gateway gateway;
    private Dashboard dashboard;
    private EdgeSecurity edgeSecurity;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public void setOwnerGroup(String ownerGroup) {
        this.ownerGroup = ownerGroup;
    }

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    public void setAdminRoleArn(String adminRoleArn) {
        this.adminRoleArn = adminRoleArn;
    }

    public String getVpcHostedZoneName() {
        return vpcHostedZoneName;
    }

    public void setVpcHostedZoneName(String vpcHostedZoneName) {
        this.vpcHostedZoneName = vpcHostedZoneName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public VpcAccessWhitelist getVpcAccessWhitelist() {
        return vpcAccessWhitelist;
    }

    public void setVpcAccessWhitelist(VpcAccessWhitelist vpcAccessWhitelist) {
        this.vpcAccessWhitelist = vpcAccessWhitelist;
    }

    public Consul getConsul() {
        return consul;
    }

    public void setConsul(Consul consul) {
        this.consul = consul;
    }

    public Vault getVault() {
        return vault;
    }

    public void setVault(Vault vault) {
        this.vault = vault;
    }

    public ManagementService getManagementService() {
        return managementService;
    }

    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public EdgeSecurity getEdgeSecurity() {
        return edgeSecurity;
    }

    public void setEdgeSecurity(EdgeSecurity edgeSecurity) {
        this.edgeSecurity = edgeSecurity;
    }
}
