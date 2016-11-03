package com.nike.cerberus.domain.cloudformation;

/**
 * RDS backup cluster outputs.
 */
public class RdsBackupOutputs {

    private String autoscalingGroupLogicalId;

    private String launchConfigurationLogicalId;

    public String getAutoscalingGroupLogicalId() {
        return autoscalingGroupLogicalId;
    }

    public RdsBackupOutputs setAutoscalingGroupLogicalId(String autoscalingGroupLogicalId) {
        this.autoscalingGroupLogicalId = autoscalingGroupLogicalId;
        return this;
    }

    public String getLaunchConfigurationLogicalId() {
        return launchConfigurationLogicalId;
    }

    public RdsBackupOutputs setLaunchConfigurationLogicalId(String launchConfigurationLogicalId) {
        this.launchConfigurationLogicalId = launchConfigurationLogicalId;
        return this;
    }
}
