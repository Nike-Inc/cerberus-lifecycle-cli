/*
 * Copyright (c) 2016 Nike Inc.
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

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Represents the input parameters for the Vault CloudFormation script.
 */
public class VaultParameters implements LaunchConfigParameters {

    private String vpcId;

    private String instanceProfileName;

    private String toolsIngressSgId;

    private String vaultServerElbSgId;

    private String vaultClientSgId;

    private String vaultServerSgId;

    private String consulClientSgId;

    private String consulServerSgId;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    private String hostedZoneId;

    private String cname;

    @JsonUnwrapped
    private SslConfigParametersDelegate sslConfigParameters = new SslConfigParametersDelegate();

    @JsonUnwrapped
    private LaunchConfigParametersDelegate launchConfigParameters = new LaunchConfigParametersDelegate();

    @JsonUnwrapped
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getVpcId() {
        return vpcId;
    }

    public VaultParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public VaultParameters setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public VaultParameters setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getVaultServerElbSgId() {
        return vaultServerElbSgId;
    }

    public VaultParameters setVaultServerElbSgId(String vaultServerElbSgId) {
        this.vaultServerElbSgId = vaultServerElbSgId;
        return this;
    }

    public String getVaultClientSgId() {
        return vaultClientSgId;
    }

    public VaultParameters setVaultClientSgId(String vaultClientSgId) {
        this.vaultClientSgId = vaultClientSgId;
        return this;
    }

    public String getVaultServerSgId() {
        return vaultServerSgId;
    }

    public VaultParameters setVaultServerSgId(String vaultServerSgId) {
        this.vaultServerSgId = vaultServerSgId;
        return this;
    }

    public String getConsulClientSgId() {
        return consulClientSgId;
    }

    public VaultParameters setConsulClientSgId(String consulClientSgId) {
        this.consulClientSgId = consulClientSgId;
        return this;
    }

    public String getConsulServerSgId() {
        return consulServerSgId;
    }

    public VaultParameters setConsulServerSgId(String consulServerSgId) {
        this.consulServerSgId = consulServerSgId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public VaultParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public VaultParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public VaultParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public VaultParameters setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
        return this;
    }

    public String getCname() {
        return cname;
    }

    public VaultParameters setCname(String cname) {
        this.cname = cname;
        return this;
    }

    @Override
    public SslConfigParametersDelegate getSslConfigParameters() {
        return sslConfigParameters;
    }

    public VaultParameters setSslConfigParameters(SslConfigParametersDelegate sslConfigParameters) {
        this.sslConfigParameters = sslConfigParameters;
        return this;
    }

    @Override
    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public VaultParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public VaultParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }
}
