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
 * Consul CloudFormation stack inputs
 */
public class ConsulParameters implements LaunchConfigParameters {

    private String instanceProfileName;

    private String consulClientSgId;

    private String consulServerSgId;

    private String toolsIngressSgId;

    private String vpcId;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    @JsonUnwrapped
    private SslConfigParametersDelegate sslConfigParameters = new SslConfigParametersDelegate();

    @JsonUnwrapped
    private LaunchConfigParametersDelegate launchConfigParameters = new LaunchConfigParametersDelegate();

    @JsonUnwrapped
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getInstanceProfileName() {
        return instanceProfileName;
    }

    public ConsulParameters setInstanceProfileName(String instanceProfileName) {
        this.instanceProfileName = instanceProfileName;
        return this;
    }

    public String getConsulClientSgId() {
        return consulClientSgId;
    }

    public ConsulParameters setConsulClientSgId(String consulClientSgId) {
        this.consulClientSgId = consulClientSgId;
        return this;
    }

    public String getConsulServerSgId() {
        return consulServerSgId;
    }

    public ConsulParameters setConsulServerSgId(String consulServerSgId) {
        this.consulServerSgId = consulServerSgId;
        return this;
    }

    public String getToolsIngressSgId() {
        return toolsIngressSgId;
    }

    public ConsulParameters setToolsIngressSgId(String toolsIngressSgId) {
        this.toolsIngressSgId = toolsIngressSgId;
        return this;
    }

    public String getVpcId() {
        return vpcId;
    }

    public ConsulParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public ConsulParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public ConsulParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public ConsulParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    @Override
    public SslConfigParametersDelegate getSslConfigParameters() {
        return sslConfigParameters;
    }

    @Override
    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public ConsulParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public ConsulParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }
}
