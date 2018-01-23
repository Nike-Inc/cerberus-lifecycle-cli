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
 * Represents the security group stack outputs.
 */
public class SecurityGroupOutputs {

    private Integer cmsDbPort;

    private String cmsSgId;

    private String cmsDbSgId;

    private String loadBalancerSgId;

    private String whitelistIngressSgId;

    public Integer getCmsDbPort() {
        return cmsDbPort;
    }

    public SecurityGroupOutputs setCmsDbPort(Integer cmsDbPort) {
        this.cmsDbPort = cmsDbPort;
        return this;
    }

    public String getCmsSgId() {
        return cmsSgId;
    }

    public SecurityGroupOutputs setCmsSgId(String cmsSgId) {
        this.cmsSgId = cmsSgId;
        return this;
    }

    public String getCmsDbSgId() {
        return cmsDbSgId;
    }

    public SecurityGroupOutputs setCmsDbSgId(String cmsDbSgId) {
        this.cmsDbSgId = cmsDbSgId;
        return this;
    }

    public String getLoadBalancerSgId() {
        return loadBalancerSgId;
    }

    public SecurityGroupOutputs setLoadBalancerSgId(String loadBalancerSgId) {
        this.loadBalancerSgId = loadBalancerSgId;
        return this;
    }

    public String getWhitelistIngressSgId() {
        return whitelistIngressSgId;
    }

    public SecurityGroupOutputs setWhitelistIngressSgId(String whitelistIngressSgId) {
        this.whitelistIngressSgId = whitelistIngressSgId;
        return this;
    }
}
