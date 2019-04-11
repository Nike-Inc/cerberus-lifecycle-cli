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
 * Represents the load balancer stack outputs.
 */
public class LoadBalancerOutputs {

    private String cmsTargetGroup;

    private String loadBalancerAccessLogBucket;

    private String loadBalancerDnsName;

    private String loadBalancerPhysicalId;

    public String getCmsTargetGroup() {
        return cmsTargetGroup;
    }

    public LoadBalancerOutputs setCmsTargetGroup(String cmsTargetGroup) {
        this.cmsTargetGroup = cmsTargetGroup;
        return this;
    }

    public String getLoadBalancerAccessLogBucket() {
        return loadBalancerAccessLogBucket;
    }

    public LoadBalancerOutputs setLoadBalancerAccessLogBucket(String loadBalancerAccessLogBucket) {
        this.loadBalancerAccessLogBucket = loadBalancerAccessLogBucket;
        return this;
    }

    public String getLoadBalancerDnsName() {
        return loadBalancerDnsName;
    }

    public LoadBalancerOutputs setLoadBalancerDnsName(String loadBalancerDnsName) {
        this.loadBalancerDnsName = loadBalancerDnsName;
        return this;
    }

    public String getLoadBalancerPhysicalId() {
        return loadBalancerPhysicalId;
    }

    public void setLoadBalancerPhysicalId(String loadBalancerPhysicalId) {
        this.loadBalancerPhysicalId = loadBalancerPhysicalId;
    }
}
