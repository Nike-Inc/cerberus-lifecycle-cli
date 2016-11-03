package com.nike.cerberus.domain.cloudformation;

/**
 * Represents output parameters for the Vault CloudFormation script.
 */
public class VaultOutputs {

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

    public VaultOutputs setAutoscalingGroupLogicalId(String autoscalingGroupLogicalId) {
        this.autoscalingGroupLogicalId = autoscalingGroupLogicalId;
        return this;
    }

    public String getLaunchConfigurationLogicalId() {
        return launchConfigurationLogicalId;
    }

    public VaultOutputs setLaunchConfigurationLogicalId(String launchConfigurationLogicalId) {
        this.launchConfigurationLogicalId = launchConfigurationLogicalId;
        return this;
    }

    public String getElbLogicalId() {
        return elbLogicalId;
    }

    public VaultOutputs setElbLogicalId(String elbLogicalId) {
        this.elbLogicalId = elbLogicalId;
        return this;
    }

    public String getElbCanonicalHostedZoneNameId() {
        return elbCanonicalHostedZoneNameId;
    }

    public VaultOutputs setElbCanonicalHostedZoneNameId(String elbCanonicalHostedZoneNameId) {
        this.elbCanonicalHostedZoneNameId = elbCanonicalHostedZoneNameId;
        return this;
    }

    public String getElbDnsName() {
        return elbDnsName;
    }

    public VaultOutputs setElbDnsName(String elbDnsName) {
        this.elbDnsName = elbDnsName;
        return this;
    }

    public String getElbSourceSecurityGroupName() {
        return elbSourceSecurityGroupName;
    }

    public VaultOutputs setElbSourceSecurityGroupName(String elbSourceSecurityGroupName) {
        this.elbSourceSecurityGroupName = elbSourceSecurityGroupName;
        return this;
    }

    public String getElbSourceSecurityGroupOwnerAlias() {
        return elbSourceSecurityGroupOwnerAlias;
    }

    public VaultOutputs setElbSourceSecurityGroupOwnerAlias(String elbSourceSecurityGroupOwnerAlias) {
        this.elbSourceSecurityGroupOwnerAlias = elbSourceSecurityGroupOwnerAlias;
        return this;
    }
}
