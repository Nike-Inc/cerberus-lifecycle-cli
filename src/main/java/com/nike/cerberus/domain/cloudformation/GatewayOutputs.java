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
 * Gateway CloudFormation stack outputs
 */
public class GatewayOutputs {

    private String autoscalingGroupLogicalId;

    private String launchConfigurationLogicalId;

    private String elbLogicalId;

    private String elbCanonicalHostedZoneName;

    private String elbCanonicalHostedZoneNameId;

    private String elbDnsName;

    private String elbSourceSecurityGroupName;

    private String elbSourceSecurityGroupOwnerAlias;

    private String cloudFrontDistributionId;

    private String cloudFrontDistributionDomainName;

    private String cloudFrontAccessLogBucket;

    private String whiteListIPSetID;

    private String manualBlockIPSetID;

    private String autoBlockIPSetID;

    public String getAutoscalingGroupLogicalId() {
        return autoscalingGroupLogicalId;
    }

    public GatewayOutputs setAutoscalingGroupLogicalId(String autoscalingGroupLogicalId) {
        this.autoscalingGroupLogicalId = autoscalingGroupLogicalId;
        return this;
    }

    public String getLaunchConfigurationLogicalId() {
        return launchConfigurationLogicalId;
    }

    public GatewayOutputs setLaunchConfigurationLogicalId(String launchConfigurationLogicalId) {
        this.launchConfigurationLogicalId = launchConfigurationLogicalId;
        return this;
    }

    public String getElbLogicalId() {
        return elbLogicalId;
    }

    public GatewayOutputs setElbLogicalId(String elbLogicalId) {
        this.elbLogicalId = elbLogicalId;
        return this;
    }

    public String getElbCanonicalHostedZoneName() {
        return elbCanonicalHostedZoneName;
    }

    public GatewayOutputs setElbCanonicalHostedZoneName(String elbCanonicalHostedZoneName) {
        this.elbCanonicalHostedZoneName = elbCanonicalHostedZoneName;
        return this;
    }

    public String getElbCanonicalHostedZoneNameId() {
        return elbCanonicalHostedZoneNameId;
    }

    public GatewayOutputs setElbCanonicalHostedZoneNameId(String elbCanonicalHostedZoneNameId) {
        this.elbCanonicalHostedZoneNameId = elbCanonicalHostedZoneNameId;
        return this;
    }

    public String getElbDnsName() {
        return elbDnsName;
    }

    public GatewayOutputs setElbDnsName(String elbDnsName) {
        this.elbDnsName = elbDnsName;
        return this;
    }

    public String getElbSourceSecurityGroupName() {
        return elbSourceSecurityGroupName;
    }

    public GatewayOutputs setElbSourceSecurityGroupName(String elbSourceSecurityGroupName) {
        this.elbSourceSecurityGroupName = elbSourceSecurityGroupName;
        return this;
    }

    public String getElbSourceSecurityGroupOwnerAlias() {
        return elbSourceSecurityGroupOwnerAlias;
    }

    public GatewayOutputs setElbSourceSecurityGroupOwnerAlias(String elbSourceSecurityGroupOwnerAlias) {
        this.elbSourceSecurityGroupOwnerAlias = elbSourceSecurityGroupOwnerAlias;
        return this;
    }

    public String getCloudFrontDistributionId() {
        return cloudFrontDistributionId;
    }

    public GatewayOutputs setCloudFrontDistributionId(String cloudFrontDistributionId) {
        this.cloudFrontDistributionId = cloudFrontDistributionId;
        return this;
    }

    public String getCloudFrontDistributionDomainName() {
        return cloudFrontDistributionDomainName;
    }

    public GatewayOutputs setCloudFrontDistributionDomainName(String cloudFrontDistributionDomainName) {
        this.cloudFrontDistributionDomainName = cloudFrontDistributionDomainName;
        return this;
    }

    public String getCloudFrontAccessLogBucket() {
        return cloudFrontAccessLogBucket;
    }

    public GatewayOutputs setCloudFrontAccessLogBucket(String cloudFrontAccessLogBucket) {
        this.cloudFrontAccessLogBucket = cloudFrontAccessLogBucket;
        return this;
    }

    public String getWhiteListIPSetID() {
        return whiteListIPSetID;
    }

    public GatewayOutputs setWhiteListIPSetID(String whiteListIPSetID) {
        this.whiteListIPSetID = whiteListIPSetID;
        return this;
    }

    public String getManualBlockIPSetID() {
        return manualBlockIPSetID;
    }

    public GatewayOutputs setManualBlockIPSetID(String manualBlockIPSetID) {
        this.manualBlockIPSetID = manualBlockIPSetID;
        return this;
    }

    public String getAutoBlockIPSetID() {
        return autoBlockIPSetID;
    }

    public GatewayOutputs setAutoBlockIPSetID(String autoBlockIPSetID) {
        this.autoBlockIPSetID = autoBlockIPSetID;
        return this;
    }
}
