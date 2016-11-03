package com.nike.cerberus.domain.cloudformation;

/**
 * Consul CloudFormation stack outputs
 */
public class ConsulOutputs {

    private String autoscalingGroupLogicalId;

    private String launchConfigurationLogicalId;

    public String getAutoscalingGroupLogicalId() {
        return autoscalingGroupLogicalId;
    }

    public ConsulOutputs setAutoscalingGroupLogicalId(String autoscalingGroupLogicalId) {
        this.autoscalingGroupLogicalId = autoscalingGroupLogicalId;
        return this;
    }

    public String getLaunchConfigurationLogicalId() {
        return launchConfigurationLogicalId;
    }

    public ConsulOutputs setLaunchConfigurationLogicalId(String launchConfigurationLogicalId) {
        this.launchConfigurationLogicalId = launchConfigurationLogicalId;
        return this;
    }
}
