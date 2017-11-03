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

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Input parameters for the Gateway CloudFormation template.
 */
public class LoadBalancerParameters implements LaunchConfigParameters {

    private String vpcId;

    private String instanceProfileName;

    private String gatewayElbSgId;

    private String gatewayServerSgId;

    private String toolsIngressSgId;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    private String hostedZoneId;

    private String hostname;

    private String wafLambdaBucket;

    private String wafLambdaKey;

    private String cloudFrontLogProcessorLambdaIamRoleArn;

    @JsonUnwrapped
    private SslConfigParametersDelegate sslConfigParameters = new SslConfigParametersDelegate();

    @JsonUnwrapped
    private LaunchConfigParametersDelegate launchConfigParameters = new LaunchConfigParametersDelegate();

    @JsonUnwrapped
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getVpcId() {
        return vpcId;
    }

    public LoadBalancerParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public LoadBalancerParameters setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
        return this;
    }

    public String getGatewayElbSgId() {
        return gatewayElbSgId;
    }

    public LoadBalancerParameters setGatewayElbSgId(String gatewayElbSgId) {
        this.gatewayElbSgId = gatewayElbSgId;
        return this;
    }

    public String getGatewayServerSgId() {
        return gatewayServerSgId;
    }

    public LoadBalancerParameters setGatewayServerSgId(String gatewayServerSgId) {
        this.gatewayServerSgId = gatewayServerSgId;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public LoadBalancerParameters setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public LoadBalancerParameters setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public LoadBalancerParameters setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    @Override
    public SslConfigParametersDelegate getSslConfigParameters() {
        return sslConfigParameters;
    }

    public LoadBalancerParameters setSslConfigParameters(SslConfigParametersDelegate sslConfigParameters) {
        this.sslConfigParameters = sslConfigParameters;
        return this;
    }

    @Override
    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public LoadBalancerParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public LoadBalancerParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }

    public String getWafLambdaBucket() {
        return wafLambdaBucket;
    }

    public LoadBalancerParameters setWafLambdaBucket(String wafLambdaBucket) {
        this.wafLambdaBucket = wafLambdaBucket;
        return this;
    }

    public String getWafLambdaKey() {
        return wafLambdaKey;
    }

    public LoadBalancerParameters setWafLambdaKey(String wafLambdaKey) {
        this.wafLambdaKey = wafLambdaKey;
        return this;
    }

    public String getCloudFrontLogProcessorLambdaIamRoleArn() {
        return cloudFrontLogProcessorLambdaIamRoleArn;
    }

    public LoadBalancerParameters setCloudFrontLogProcessorLambdaIamRoleArn(String cloudFrontLogProcessorLambdaIamRoleArn) {
        this.cloudFrontLogProcessorLambdaIamRoleArn = cloudFrontLogProcessorLambdaIamRoleArn;
        return this;
    }
}
