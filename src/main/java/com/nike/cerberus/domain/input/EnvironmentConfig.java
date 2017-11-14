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


import java.util.List;
import java.util.Map;

/**
 * Stores all YAML data for a given Cerberus environment
 */
public class EnvironmentConfig {

    private String version;
    private String environmentName;
    private String primaryRegion;
    private Map<String, String> globalTags;
    private String adminRoleArn;
    private String baseDomainName;
    private String edgeDomainNameOverride;
    private String originDomainNameOverride;
    private String loadBalancerDomainNameOverride;
    private String loadBalancerSslPolicyOverride;
    private List<String> additionalSubjectNames;
    private String hostedZoneId;
    private VpcAccessWhitelist vpcAccessWhitelist;
    private boolean generateKeysAndCerts;
    private String acmeApiUrl;
    private boolean enableLeCertFix;
    private String acmeContactEmail;
    private String localFolderToStoreCerts;
    private ManagementService managementService;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    public Map<String, String> getGlobalTags() {
        return globalTags;
    }

    public void setGlobalTags(Map<String, String> globalTags) {
        this.globalTags = globalTags;
    }

    public String getAdminRoleArn() {
        return adminRoleArn;
    }

    public void setAdminRoleArn(String adminRoleArn) {
        this.adminRoleArn = adminRoleArn;
    }

    public String getBaseDomainName() {
        return baseDomainName;
    }

    public void setBaseDomainName(String baseDomainName) {
        this.baseDomainName = baseDomainName;
    }

    public String getEdgeDomainNameOverride() {
        return edgeDomainNameOverride;
    }

    public void setEdgeDomainNameOverride(String edgeDomainNameOverride) {
        this.edgeDomainNameOverride = edgeDomainNameOverride;
    }

    public String getOriginDomainNameOverride() {
        return originDomainNameOverride;
    }

    public void setOriginDomainNameOverride(String originDomainNameOverride) {
        this.originDomainNameOverride = originDomainNameOverride;
    }

    public String getLoadBalancerDomainNameOverride() {
        return loadBalancerDomainNameOverride;
    }

    public void setLoadBalancerDomainNameOverride(String loadBalancerDomainNameOverride) {
        this.loadBalancerDomainNameOverride = loadBalancerDomainNameOverride;
    }

    public String getLoadBalancerSslPolicyOverride() {
        return loadBalancerSslPolicyOverride;
    }

    public void setLoadBalancerSslPolicyOverride(String loadBalancerSslPolicyOverride) {
        this.loadBalancerSslPolicyOverride = loadBalancerSslPolicyOverride;
    }

    public List<String> getAdditionalSubjectNames() {
        return additionalSubjectNames;
    }

    public void setAdditionalSubjectNames(List<String> additionalSubjectNames) {
        this.additionalSubjectNames = additionalSubjectNames;
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

    public boolean isGenerateKeysAndCerts() {
        return generateKeysAndCerts;
    }

    public void setGenerateKeysAndCerts(boolean generateKeysAndCerts) {
        this.generateKeysAndCerts = generateKeysAndCerts;
    }

    public String getAcmeApiUrl() {
        return acmeApiUrl;
    }

    public void setAcmeApiUrl(String acmeApiUrl) {
        this.acmeApiUrl = acmeApiUrl;
    }

    public boolean isEnableLeCertFix() {
        return enableLeCertFix;
    }

    public void setEnableLeCertFix(boolean enableLeCertFix) {
        this.enableLeCertFix = enableLeCertFix;
    }

    public String getAcmeContactEmail() {
        return acmeContactEmail;
    }

    public void setAcmeContactEmail(String acmeContactEmail) {
        this.acmeContactEmail = acmeContactEmail;
    }

    public String getLocalFolderToStoreCerts() {
        return localFolderToStoreCerts;
    }

    public void setLocalFolderToStoreCerts(String localFolderToStoreCerts) {
        this.localFolderToStoreCerts = localFolderToStoreCerts;
    }

    public ManagementService getManagementService() {
        return managementService;
    }

    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }
}
