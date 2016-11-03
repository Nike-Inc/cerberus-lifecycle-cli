package com.nike.cerberus.domain.cloudformation;

/**
 * Stack outputs from the CMS CloudFormation template.
 */
public class CmsOutputs {

    private String autoscalingGroupLogicalId;

    private String launchConfigurationLogicalId;

    private String elbLogicalId;

    private String elbCanonicalHostedZoneNameId;

    private String elbDnsName;

    private String elbSourceSecurityGroupName;

    private String elbSourceSecurityGroupOwnerAlias;

    public String getAutoscalingGroupLogicalId() {
        return autoscalingGroupLogicalId;
    }

    public CmsOutputs setAutoscalingGroupLogicalId(String autoscalingGroupLogicalId) {
        this.autoscalingGroupLogicalId = autoscalingGroupLogicalId;
        return this;
    }

    public String getLaunchConfigurationLogicalId() {
        return launchConfigurationLogicalId;
    }

    public CmsOutputs setLaunchConfigurationLogicalId(String launchConfigurationLogicalId) {
        this.launchConfigurationLogicalId = launchConfigurationLogicalId;
        return this;
    }

    public String getElbLogicalId() {
        return elbLogicalId;
    }

    public CmsOutputs setElbLogicalId(String elbLogicalId) {
        this.elbLogicalId = elbLogicalId;
        return this;
    }

    public String getElbCanonicalHostedZoneNameId() {
        return elbCanonicalHostedZoneNameId;
    }

    public CmsOutputs setElbCanonicalHostedZoneNameId(String elbCanonicalHostedZoneNameId) {
        this.elbCanonicalHostedZoneNameId = elbCanonicalHostedZoneNameId;
        return this;
    }

    public String getElbDnsName() {
        return elbDnsName;
    }

    public CmsOutputs setElbDnsName(String elbDnsName) {
        this.elbDnsName = elbDnsName;
        return this;
    }

    public String getElbSourceSecurityGroupName() {
        return elbSourceSecurityGroupName;
    }

    public CmsOutputs setElbSourceSecurityGroupName(String elbSourceSecurityGroupName) {
        this.elbSourceSecurityGroupName = elbSourceSecurityGroupName;
        return this;
    }

    public String getElbSourceSecurityGroupOwnerAlias() {
        return elbSourceSecurityGroupOwnerAlias;
    }

    public CmsOutputs setElbSourceSecurityGroupOwnerAlias(String elbSourceSecurityGroupOwnerAlias) {
        this.elbSourceSecurityGroupOwnerAlias = elbSourceSecurityGroupOwnerAlias;
        return this;
    }
}
