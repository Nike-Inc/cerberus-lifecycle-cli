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
 * Represents the vpc stack outputs.
 */
public class VpcOutputs {

    private Integer subnetCidrBlockForAz1;

    private String subnetCidrBlockForAz2;

    private String subnetCidrBlockForAz3;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    private String vpcId;

    public Integer getSubnetCidrBlockForAz1() {
        return subnetCidrBlockForAz1;
    }

    public VpcOutputs setSubnetCidrBlockForAz1(Integer subnetCidrBlockForAz1) {
        this.subnetCidrBlockForAz1 = subnetCidrBlockForAz1;
        return this;
    }

    public String getSubnetCidrBlockForAz2() {
        return subnetCidrBlockForAz2;
    }

    public VpcOutputs setSubnetCidrBlockForAz2(String subnetCidrBlockForAz2) {
        this.subnetCidrBlockForAz2 = subnetCidrBlockForAz2;
        return this;
    }

    public String getSubnetCidrBlockForAz3() {
        return subnetCidrBlockForAz3;
    }

    public VpcOutputs setSubnetCidrBlockForAz3(String subnetCidrBlockForAz3) {
        this.subnetCidrBlockForAz3 = subnetCidrBlockForAz3;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public VpcOutputs setVpcSubnetForAz1(String vpcSubnetForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public VpcOutputs setVpcSubnetForAz2(String vpcSubnetForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public VpcOutputs setVpcSubnetForAz3(String vpcSubnetForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetForAz3;
        return this;
    }

    public String getVpcId() {
        return vpcId;
    }

    public VpcOutputs setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }
}
