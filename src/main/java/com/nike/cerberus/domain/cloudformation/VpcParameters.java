/*
 * Copyright (c) 2019 Nike, Inc.
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
 * Represents the vpc stack inputs.
 */
public class VpcParameters {

    private String az1;

    private String az2;

    private String az3;

    private String internetGatewayCidrBlock;

    private String subnetCidrBlockForAz1;

    private String subnetCidrBlockForAz2;

    private String subnetCidrBlockForAz3;

    private String vpcCidrBlock;

    private String environmentName;

    public String getAz1() {
        return az1;
    }

    public VpcParameters setAz1(String az1) {
        this.az1 = az1;
        return this;
    }

    public String getAz2() {
        return az2;
    }

    public VpcParameters setAz2(String az2) {
        this.az2 = az2;
        return this;
    }

    public String getAz3() {
        return az3;
    }

    public VpcParameters setAz3(String az3) {
        this.az3 = az3;
        return this;
    }

    public String getInternetGatewayCidrBlock() {
        return internetGatewayCidrBlock;
    }

    public VpcParameters setInternetGatewayCidrBlock(String internetGatewayCidrBlock) {
        this.internetGatewayCidrBlock = internetGatewayCidrBlock;
        return this;
    }

    public String getSubnetCidrBlockForAz1() {
        return subnetCidrBlockForAz1;
    }

    public VpcParameters setSubnetCidrBlockForAz1(String subnetCidrBlockForAz1) {
        this.subnetCidrBlockForAz1 = subnetCidrBlockForAz1;
        return this;
    }

    public String getSubnetCidrBlockForAz2() {
        return subnetCidrBlockForAz2;
    }

    public VpcParameters setSubnetCidrBlockForAz2(String subnetCidrBlockForAz2) {
        this.subnetCidrBlockForAz2 = subnetCidrBlockForAz2;
        return this;
    }

    public String getSubnetCidrBlockForAz3() {
        return subnetCidrBlockForAz3;
    }

    public VpcParameters setSubnetCidrBlockForAz3(String subnetCidrBlockForAz3) {
        this.subnetCidrBlockForAz3 = subnetCidrBlockForAz3;
        return this;
    }

    public String getVpcCidrBlock() {
        return vpcCidrBlock;
    }

    public VpcParameters setVpcCidrBlock(String vpcCidrBlock) {
        this.vpcCidrBlock = vpcCidrBlock;
        return this;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public VpcParameters setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }
}
