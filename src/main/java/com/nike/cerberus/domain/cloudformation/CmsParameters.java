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
 * Input parameters for the CMS CloudFormation template.
 */
public class CmsParameters implements LaunchConfigParameters {

    private String cmsIamRoleName;

    private String loadBalancerStackName;

    private String sgStackName;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    @JsonUnwrapped
    private LaunchConfigParametersDelegate launchConfigParameters = new LaunchConfigParametersDelegate();

    public String getCmsIamRoleName() {
        return cmsIamRoleName;
    }

    public CmsParameters setCmsIamRoleName(String cmsIamRoleName) {
        this.cmsIamRoleName = cmsIamRoleName;
        return this;
    }

    public String getLoadBalancerStackName() {
        return loadBalancerStackName;
    }

    public CmsParameters setLoadBalancerStackName(String loadBalancerStackName) {
        this.loadBalancerStackName = loadBalancerStackName;
        return this;
    }

    public String getSgStackName() {
        return sgStackName;
    }

    public CmsParameters setSgStackName(String sgStackName) {
        this.sgStackName = sgStackName;
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

    public LaunchConfigParametersDelegate getLaunchConfigParameters() {
        return launchConfigParameters;
    }

    public CmsParameters setLaunchConfigParameters(LaunchConfigParametersDelegate launchConfigParameters) {
        this.launchConfigParameters = launchConfigParameters;
        return this;
    }
}
