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

public class LambdaInputParameters {

    private String lambdaJarS3Bucket;
    private String lambdaJarS3Key;
    private String instanceProfileArn;
    private String vaultIngressSecurityGroup;
    private String vpcSubnetIdForAz1;
    private String vpcSubnetIdForAz2;
    private String vpcSubnetIdForAz3;
    private String tagName;
    private String tagEmail;
    private String tagCostcenter;

    public String getLambdaJarS3Bucket() {
        return lambdaJarS3Bucket;
    }

    public LambdaInputParameters setLambdaJarS3Bucket(String lambdaJarS3Bucket) {
        this.lambdaJarS3Bucket = lambdaJarS3Bucket;
        return this;
    }

    public String getLambdaJarS3Key() {
        return lambdaJarS3Key;
    }

    public LambdaInputParameters setLambdaJarS3Key(String lambdaJarS3Key) {
        this.lambdaJarS3Key = lambdaJarS3Key;
        return this;
    }

    public String getInstanceProfileArn() {
        return instanceProfileArn;
    }

    public LambdaInputParameters setInstanceProfileArn(String instanceProfileArn) {
        this.instanceProfileArn = instanceProfileArn;
        return this;
    }

    public String getVaultIngressSecurityGroup() {
        return vaultIngressSecurityGroup;
    }

    public LambdaInputParameters setVaultIngressSecurityGroup(String vaultIngressSecurityGroup) {
        this.vaultIngressSecurityGroup = vaultIngressSecurityGroup;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public LambdaInputParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public LambdaInputParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public LambdaInputParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getTagName() {
        return tagName;
    }

    public LambdaInputParameters setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    public String getTagEmail() {
        return tagEmail;
    }

    public LambdaInputParameters setTagEmail(String tagEmail) {
        this.tagEmail = tagEmail;
        return this;
    }

    public String getTagCostcenter() {
        return tagCostcenter;
    }

    public LambdaInputParameters setTagCostcenter(String tagCostcenter) {
        this.tagCostcenter = tagCostcenter;
        return this;
    }
}
