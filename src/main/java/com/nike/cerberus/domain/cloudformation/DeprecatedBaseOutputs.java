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

package com.nike.cerberus.domain.cloudformation;

/**
 * Represents the base stack outputs.
 *
 * @deprecated TODO: remove this class when old backup and restore is removed
 * use {@link com.nike.cerberus.domain.cloudformation.BaseOutputs} for new environments
 */
@Deprecated
public class DeprecatedBaseOutputs {

    private String vpcId;

    private String gatewayIamRoleArn;

    private String cmsIamRoleArn;

    private String consulIamRoleArn;

    private String vaultIamRoleArn;

    private String cloudFrontLogProcessorLambdaIamRoleArn;

    private String gatewayInstanceProfileName;

    private String cmsInstanceProfileName;

    private String consulInstanceProfileName;

    private String vaultInstanceProfileName;

    private String toolsIngressSgId;

    private String gatewayElbSgId;

    private String gatewayServerSgId;

    private String cmsElbSgId;

    private String cmsSgId;

    private String cmsDbSgId;

    private String vaultServerElbSgId;

    private String vaultClientSgId;

    private String vaultServerSgId;

    private String consulClientSgId;

    private String consulServerSgId;

    private String configFileKeyId;

    private String dashboardBucketName;

    private String dashboardBucketWebsiteUrl;

    private String configBucketName;

    private String configBucketDomainName;

    private String cmsDbId;

    private String cmsDbAddress;

    private String cmsDbPort;

    private String cmsDbJdbcConnectionString;

    private String vpcHostedZoneId;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    private String cmsKmsPolicyId;

    private String subnetCidrBlockForAz1;

    private String subnetCidrBlockForAz2;

    private String subnetCidrBlockForAz3;

    public String getVpcId() {
        return vpcId;
    }

    public DeprecatedBaseOutputs setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getGatewayIamRoleArn() {
        return gatewayIamRoleArn;
    }

    public DeprecatedBaseOutputs setGatewayIamRoleArn(String gatewayIamRoleArn) {
        this.gatewayIamRoleArn = gatewayIamRoleArn;
        return this;
    }

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public DeprecatedBaseOutputs setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
        return this;
    }

    public String getConsulIamRoleArn() {
        return consulIamRoleArn;
    }

    public DeprecatedBaseOutputs setConsulIamRoleArn(String consulIamRoleArn) {
        this.consulIamRoleArn = consulIamRoleArn;
        return this;
    }

    public String getVaultIamRoleArn() {
        return vaultIamRoleArn;
    }

    public DeprecatedBaseOutputs setVaultIamRoleArn(String vaultIamRoleArn) {
        this.vaultIamRoleArn = vaultIamRoleArn;
        return this;
    }

    public String getCloudFrontLogProcessorLambdaIamRoleArn() {
        return cloudFrontLogProcessorLambdaIamRoleArn;
    }

    public void setCloudFrontLogProcessorLambdaIamRoleArn(String cloudFrontLogProcessorLambdaIamRoleArn) {
        this.cloudFrontLogProcessorLambdaIamRoleArn = cloudFrontLogProcessorLambdaIamRoleArn;
    }

    public String getGatewayInstanceProfileName() {
        return gatewayInstanceProfileName;
    }

    public DeprecatedBaseOutputs setGatewayInstanceProfileName(String gatewayInstanceProfileName) {
        this.gatewayInstanceProfileName = gatewayInstanceProfileName;
        return this;
    }

    public String getCmsInstanceProfileName() {
        return cmsInstanceProfileName;
    }

    public DeprecatedBaseOutputs setCmsInstanceProfileName(String cmsInstanceProfileName) {
        this.cmsInstanceProfileName = cmsInstanceProfileName;
        return this;
    }

    public String getConsulInstanceProfileName() {
        return consulInstanceProfileName;
    }

    public DeprecatedBaseOutputs setConsulInstanceProfileName(String consulInstanceProfileName) {
        this.consulInstanceProfileName = consulInstanceProfileName;
        return this;
    }

    public String getVaultInstanceProfileName() {
        return vaultInstanceProfileName;
    }

    public DeprecatedBaseOutputs setVaultInstanceProfileName(String vaultInstanceProfileName) {
        this.vaultInstanceProfileName = vaultInstanceProfileName;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public DeprecatedBaseOutputs setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getGatewayElbSgId() {
        return gatewayElbSgId;
    }

    public DeprecatedBaseOutputs setGatewayElbSgId(String gatewayElbSgId) {
        this.gatewayElbSgId = gatewayElbSgId;
        return this;
    }

    public String getGatewayServerSgId() {
        return gatewayServerSgId;
    }

    public DeprecatedBaseOutputs setGatewayServerSgId(String gatewayServerSgId) {
        this.gatewayServerSgId = gatewayServerSgId;
        return this;
    }

    public String getCmsElbSgId() {
        return cmsElbSgId;
    }

    public DeprecatedBaseOutputs setCmsElbSgId(String cmsElbSgId) {
        this.cmsElbSgId = cmsElbSgId;
        return this;
    }

    public String getCmsSgId() {
        return cmsSgId;
    }

    public DeprecatedBaseOutputs setCmsSgId(String cmsSgId) {
        this.cmsSgId = cmsSgId;
        return this;
    }

    public String getCmsDbSgId() {
        return cmsDbSgId;
    }

    public DeprecatedBaseOutputs setCmsDbSgId(String cmsDbSgId) {
        this.cmsDbSgId = cmsDbSgId;
        return this;
    }

    public String getVaultServerElbSgId() {
        return vaultServerElbSgId;
    }

    public DeprecatedBaseOutputs setVaultServerElbSgId(String vaultServerElbSgId) {
        this.vaultServerElbSgId = vaultServerElbSgId;
        return this;
    }

    public String getVaultClientSgId() {
        return vaultClientSgId;
    }

    public DeprecatedBaseOutputs setVaultClientSgId(String vaultClientSgId) {
        this.vaultClientSgId = vaultClientSgId;
        return this;
    }

    public String getVaultServerSgId() {
        return vaultServerSgId;
    }

    public DeprecatedBaseOutputs setVaultServerSgId(String vaultServerSgId) {
        this.vaultServerSgId = vaultServerSgId;
        return this;
    }

    public String getConsulClientSgId() {
        return consulClientSgId;
    }

    public DeprecatedBaseOutputs setConsulClientSgId(String consulClientSgId) {
        this.consulClientSgId = consulClientSgId;
        return this;
    }

    public String getConsulServerSgId() {
        return consulServerSgId;
    }

    public DeprecatedBaseOutputs setConsulServerSgId(String consulServerSgId) {
        this.consulServerSgId = consulServerSgId;
        return this;
    }

    public String getConfigFileKeyId() {
        return configFileKeyId;
    }

    public DeprecatedBaseOutputs setConfigFileKeyId(String configFileKeyId) {
        this.configFileKeyId = configFileKeyId;
        return this;
    }

    public String getDashboardBucketName() {
        return dashboardBucketName;
    }

    public DeprecatedBaseOutputs setDashboardBucketName(String dashboardBucketName) {
        this.dashboardBucketName = dashboardBucketName;
        return this;
    }

    public String getDashboardBucketWebsiteUrl() {
        return dashboardBucketWebsiteUrl;
    }

    public DeprecatedBaseOutputs setDashboardBucketWebsiteUrl(String dashboardBucketWebsiteUrl) {
        this.dashboardBucketWebsiteUrl = dashboardBucketWebsiteUrl;
        return this;
    }

    public String getConfigBucketName() {
        return configBucketName;
    }

    public DeprecatedBaseOutputs setConfigBucketName(String configBucketName) {
        this.configBucketName = configBucketName;
        return this;
    }

    public String getConfigBucketDomainName() {
        return configBucketDomainName;
    }

    public DeprecatedBaseOutputs setConfigBucketDomainName(String configBucketDomainName) {
        this.configBucketDomainName = configBucketDomainName;
        return this;
    }

    public String getCmsDbId() {
        return cmsDbId;
    }

    public DeprecatedBaseOutputs setCmsDbId(String cmsDbId) {
        this.cmsDbId = cmsDbId;
        return this;
    }

    public String getCmsDbAddress() {
        return cmsDbAddress;
    }

    public DeprecatedBaseOutputs setCmsDbAddress(String cmsDbAddress) {
        this.cmsDbAddress = cmsDbAddress;
        return this;
    }

    public String getCmsDbPort() {
        return cmsDbPort;
    }

    public DeprecatedBaseOutputs setCmsDbPort(String cmsDbPort) {
        this.cmsDbPort = cmsDbPort;
        return this;
    }

    public String getCmsDbJdbcConnectionString() {
        return cmsDbJdbcConnectionString;
    }

    public DeprecatedBaseOutputs setCmsDbJdbcConnectionString(String cmsDbJdbcConnectionString) {
        this.cmsDbJdbcConnectionString = cmsDbJdbcConnectionString;
        return this;
    }

    public String getVpcHostedZoneId() {
        return vpcHostedZoneId;
    }

    public DeprecatedBaseOutputs setVpcHostedZoneId(String vpcHostedZoneId) {
        this.vpcHostedZoneId = vpcHostedZoneId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public DeprecatedBaseOutputs setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public DeprecatedBaseOutputs setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public DeprecatedBaseOutputs setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getCmsKmsPolicyId() {
        return cmsKmsPolicyId;
    }

    public DeprecatedBaseOutputs setCmsKmsPolicyId(String cmsKmsPolicyId) {
        this.cmsKmsPolicyId = cmsKmsPolicyId;
        return this;
    }

    public String getSubnetCidrBlockForAz1() {
        return subnetCidrBlockForAz1;
    }

    public DeprecatedBaseOutputs setSubnetCidrBlockForAz1(String subnetCidrBlockForAz1) {
        this.subnetCidrBlockForAz1 = subnetCidrBlockForAz1;
        return this;
    }

    public String getSubnetCidrBlockForAz2() {
        return subnetCidrBlockForAz2;
    }

    public DeprecatedBaseOutputs setSubnetCidrBlockForAz2(String subnetCidrBlockForAz2) {
        this.subnetCidrBlockForAz2 = subnetCidrBlockForAz2;
        return this;
    }

    public String getSubnetCidrBlockForAz3() {
        return subnetCidrBlockForAz3;
    }

    public DeprecatedBaseOutputs setSubnetCidrBlockForAz3(String subnetCidrBlockForAz3) {
        this.subnetCidrBlockForAz3 = subnetCidrBlockForAz3;
        return this;
    }
}
