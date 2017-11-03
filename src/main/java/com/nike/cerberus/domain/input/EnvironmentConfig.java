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

package com.nike.cerberus.domain.input;


/**
 * Stores all YAML data for a given Cerberus environment
 */
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
    private Vault vault;
    private ManagementService managementService;
    private Gateway gateway;

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
}
