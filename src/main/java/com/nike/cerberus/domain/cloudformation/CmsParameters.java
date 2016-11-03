package com.nike.cerberus.domain.cloudformation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Input parameters for the CMS CloudFormation template.
 */
public class CmsParameters implements LaunchConfigParameters {

    private String vpcId;

    private String instanceProfileName;

    private String cmsElbSgId;

    private String cmsSgId;

    private String toolsIngressSgId;

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

    public CmsParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public CmsParameters setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
        return this;
    }

    public String getCmsElbSgId() {
        return cmsElbSgId;
    }

    public CmsParameters setCmsElbSgId(String cmsElbSgId) {
        this.cmsElbSgId = cmsElbSgId;
        return this;
    }

    public String getCmsSgId() {
        return cmsSgId;
    }

    public CmsParameters setCmsSgId(String cmsSgId) {
        this.cmsSgId = cmsSgId;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public CmsParameters setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public CmsParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public CmsParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public CmsParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public CmsParameters setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
        return this;
    }

    public String getCname() {
        return cname;
    }

    public CmsParameters setCname(String cname) {
        this.cname = cname;
        return this;
    }

    @Override
    public SslConfigParametersDelegate getSslConfigParameters() {
        return sslConfigParameters;
    }

    public CmsParameters setSslConfigParameters(SslConfigParametersDelegate sslConfigParameters) {
        this.sslConfigParameters = sslConfigParameters;
        return this;
    }

    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public CmsParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public CmsParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }
}
