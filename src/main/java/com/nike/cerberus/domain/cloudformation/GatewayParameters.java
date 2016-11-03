package com.nike.cerberus.domain.cloudformation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Input parameters for the Gateway CloudFormation template.
 */
public class GatewayParameters implements LaunchConfigParameters {

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

    public GatewayParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public GatewayParameters setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
        return this;
    }

    public String getGatewayElbSgId() {
        return gatewayElbSgId;
    }

    public GatewayParameters setGatewayElbSgId(String gatewayElbSgId) {
        this.gatewayElbSgId = gatewayElbSgId;
        return this;
    }

    public String getGatewayServerSgId() {
        return gatewayServerSgId;
    }

    public GatewayParameters setGatewayServerSgId(String gatewayServerSgId) {
        this.gatewayServerSgId = gatewayServerSgId;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public GatewayParameters setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public GatewayParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public GatewayParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public GatewayParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public GatewayParameters setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public GatewayParameters setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    @Override
    public SslConfigParametersDelegate getSslConfigParameters() {
        return sslConfigParameters;
    }

    public GatewayParameters setSslConfigParameters(SslConfigParametersDelegate sslConfigParameters) {
        this.sslConfigParameters = sslConfigParameters;
        return this;
    }

    @Override
    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public GatewayParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public GatewayParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }

    public String getWafLambdaBucket() {
        return wafLambdaBucket;
    }

    public GatewayParameters setWafLambdaBucket(String wafLambdaBucket) {
        this.wafLambdaBucket = wafLambdaBucket;
        return this;
    }

    public String getWafLambdaKey() {
        return wafLambdaKey;
    }

    public GatewayParameters setWafLambdaKey(String wafLambdaKey) {
        this.wafLambdaKey = wafLambdaKey;
        return this;
    }

    public String getCloudFrontLogProcessorLambdaIamRoleArn() {
        return cloudFrontLogProcessorLambdaIamRoleArn;
    }

    public GatewayParameters setCloudFrontLogProcessorLambdaIamRoleArn(String cloudFrontLogProcessorLambdaIamRoleArn) {
        this.cloudFrontLogProcessorLambdaIamRoleArn = cloudFrontLogProcessorLambdaIamRoleArn;
        return this;
    }
}
