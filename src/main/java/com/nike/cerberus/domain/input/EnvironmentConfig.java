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

package com.nike.cerberus.domain.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.MoreCollectors.onlyElement;

/**
 * Stores all YAML data for a given Cerberus environment
 */
public class EnvironmentConfig {

    private String environmentName;
    private Map<String, String> globalTags;
    private String adminRoleArn;
    private String baseDomainName;
    private String edgeDomainNameOverride;
    private String originDomainNameOverride;
    private List<String> additionalSubjectNames;
    private String loadBalancerSslPolicyOverride;
    private String hostedZoneId;
    private boolean enableAuditLogs;
    private boolean generateKeysAndCerts;
    private String acmeApiUrl;
    private boolean enableLeCertFix;
    private String acmeContactEmail;
    private String certificateDirectory;
    private VpcAccessWhitelistInput vpcAccessWhitelist;
    private ManagementServiceInput managementService;
    private Map<String, RegionSpecificConfigurationInput> regionSpecificConfiguration = new HashMap<>();

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
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

    public List<String> getAdditionalSubjectNames() {
        return additionalSubjectNames;
    }

    public void setAdditionalSubjectNames(List<String> additionalSubjectNames) {
        this.additionalSubjectNames = additionalSubjectNames;
    }

    public String getLoadBalancerSslPolicyOverride() {
        return loadBalancerSslPolicyOverride;
    }

    public void setLoadBalancerSslPolicyOverride(String loadBalancerSslPolicyOverride) {
        this.loadBalancerSslPolicyOverride = loadBalancerSslPolicyOverride;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public boolean isEnableAuditLogs() {
        return enableAuditLogs;
    }

    public EnvironmentConfig setEnableAuditLogs(boolean enableAuditLogs) {
        this.enableAuditLogs = enableAuditLogs;
        return this;
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

    public String getCertificateDirectory() {
        return certificateDirectory;
    }

    public void setCertificateDirectory(String certificateDirectory) {
        this.certificateDirectory = certificateDirectory;
    }

    public VpcAccessWhitelistInput getVpcAccessWhitelist() {
        return vpcAccessWhitelist;
    }

    public void setVpcAccessWhitelist(VpcAccessWhitelistInput vpcAccessWhitelist) {
        this.vpcAccessWhitelist = vpcAccessWhitelist;
    }

    public ManagementServiceInput getManagementService() {
        return managementService;
    }

    public void setManagementService(ManagementServiceInput managementService) {
        this.managementService = managementService;
    }

    public Map<String, RegionSpecificConfigurationInput> getRegionSpecificConfiguration() {
        return regionSpecificConfiguration;
    }

    public void setRegionSpecificConfiguration(Map<String, RegionSpecificConfigurationInput> regionSpecificConfiguration) {
        this.regionSpecificConfiguration = regionSpecificConfiguration;
    }

    public RegionSpecificConfigurationInput getPrimaryRegionConfig() {
        return getPrimaryEntry().getValue();
    }

    public RegionSpecificConfigurationInput getRegionConfig(String region) {
        return Optional.ofNullable(getRegionSpecificConfiguration().get(region))
            .orElseThrow(() -> new RuntimeException(String
                .format("Failed to find region config for %s in region specific config", region)));
    }

    public String getPrimaryRegion() {
        return getPrimaryEntry().getKey();
    }

    private Map.Entry<String, RegionSpecificConfigurationInput> getPrimaryEntry() {
        return regionSpecificConfiguration.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().isPrimary())
            .collect(onlyElement());
    }
}
