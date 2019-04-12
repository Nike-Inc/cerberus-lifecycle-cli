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

import java.util.List;
import java.util.Map;

/**
 * POJO for representing a CloudFormation stack parameter and outputs.
 */
public class StackInfo {

    private String stackId;

    private Map<String, String> stackParameters;

    private Map<String, String> stackOutputs;

    private List<String> publicDnsForInstances;

    public String getStackId() {
        return stackId;
    }

    public StackInfo setStackId(String stackId) {
        this.stackId = stackId;
        return this;
    }

    public Map<String, String> getStackParameters() {
        return stackParameters;
    }

    public StackInfo setStackParameters(Map<String, String> stackParameters) {
        this.stackParameters = stackParameters;
        return this;
    }

    public Map<String, String> getStackOutputs() {
        return stackOutputs;
    }

    public StackInfo setStackOutputs(Map<String, String> stackOutputs) {
        this.stackOutputs = stackOutputs;
        return this;
    }

    public List<String> getPublicDnsForInstances() {
        return publicDnsForInstances;
    }

    public StackInfo setPublicDnsForInstances(List<String> publicDnsForInstances) {
        this.publicDnsForInstances = publicDnsForInstances;
        return this;
    }
}
