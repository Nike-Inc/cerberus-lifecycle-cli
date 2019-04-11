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
 * Represents the load balancer stack inputs.
 */
public class LoadBalancerParameters {

    private String sgStackName;

    private String sslCertificateArn;

    private String vpcId;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    private String sslPolicy;

    private String elasticLoadBalancingAccountId;

    public String getSgStackName() {
        return sgStackName;
    }

    public LoadBalancerParameters setSgStackName(String sgStackName) {
        this.sgStackName = sgStackName;
        return this;
    }

    public String getSslCertificateArn() {
        return sslCertificateArn;
    }

    public LoadBalancerParameters setSslCertificateArn(String sslCertificateArn) {
        this.sslCertificateArn = sslCertificateArn;
        return this;
    }

    public String getVpcId() {
        return vpcId;
    }

    public LoadBalancerParameters setVpcId(String vpcId) {
        this.vpcId = vpcId;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public LoadBalancerParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    public String getSslPolicy() {
        return sslPolicy;
    }

    public LoadBalancerParameters setSslPolicy(String sslPolicy) {
        this.sslPolicy = sslPolicy;
        return this;
    }

    public String getElasticLoadBalancingAccountId() {
        return elasticLoadBalancingAccountId;
    }

    public LoadBalancerParameters setElasticLoadBalancingAccountId(String elasticLoadBalancingAccountId) {
        this.elasticLoadBalancingAccountId = elasticLoadBalancingAccountId;
        return this;
    }
}
