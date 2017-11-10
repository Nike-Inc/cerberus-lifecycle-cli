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
 * Represents the waf stack inputs.
 */
public class WafParameters {

    private String loadBalancerStackName;

    private String wafName;

    public String getLoadBalancerStackName() {
        return loadBalancerStackName;
    }

    public WafParameters setLoadBalancerStackName(String loadBalancerStackName) {
        this.loadBalancerStackName = loadBalancerStackName;
        return this;
    }

    public String getWafName() {
        return wafName;
    }

    public WafParameters setWafName(String wafName) {
        this.wafName = wafName;
        return this;
    }
}
