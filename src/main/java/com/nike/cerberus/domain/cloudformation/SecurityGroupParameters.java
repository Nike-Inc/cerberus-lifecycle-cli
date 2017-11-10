/*
 * Copyright (c) 2017 Nike, Inc.
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
 * Represents the security group stack inputs.
 */
public class SecurityGroupParameters {

    private Integer cmsDbPort;

    private String loadBalancerCidrBlock;

    private String vpcId;

    public Integer getCmsDbPort() {
        return cmsDbPort;
    }

    public SecurityGroupParameters setCmsDbPort(Integer cmsDbPort) {
        this.cmsDbPort = cmsDbPort;
        return this;
    }

    public String getLoadBalancerCidrBlock() {
        return loadBalancerCidrBlock;
    }

    public SecurityGroupParameters setLoadBalancerCidrBlock(String loadBalancerCidrBlock) {
        this.loadBalancerCidrBlock = loadBalancerCidrBlock;
        return this;
    }

    public String getVpcId() {
        return vpcId;
    }

    public SecurityGroupParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

}
