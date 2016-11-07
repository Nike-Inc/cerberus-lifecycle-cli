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
