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
 * Represents the route53 stack outputs.
 */
public class Route53Outputs {

    private String loadBalancerDomainName;

    private String originDomainName;

    public String getLoadBalancerDomainName() {
        return loadBalancerDomainName;
    }

    public void setLoadBalancerDomainName(String loadBalancerDomainName) {
        this.loadBalancerDomainName = loadBalancerDomainName;
    }

    public String getOriginDomainName() {
        return originDomainName;
    }

    public void setOriginDomainName(String originDomainName) {
        this.originDomainName = originDomainName;
    }
}
