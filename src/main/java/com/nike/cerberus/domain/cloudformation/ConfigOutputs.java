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

public class ConfigOutputs {

    private String configBucketDomainName;

    private String configBucketName;

    private String configCmkArn;

    private String managementServiceCmkArn;

    public String getConfigBucketDomainName() {
        return configBucketDomainName;
    }

    public ConfigOutputs setConfigBucketDomainName(String configBucketDomainName) {
        this.configBucketDomainName = configBucketDomainName;
        return this;
    }

    public String getConfigBucketName() {
        return configBucketName;
    }

    public ConfigOutputs setConfigBucketName(String configBucketName) {
        this.configBucketName = configBucketName;
        return this;
    }

    public String getConfigCmkArn() {
        return configCmkArn;
    }

    public void setConfigCmkArn(String configCmkArn) {
        this.configCmkArn = configCmkArn;
    }

    public String getManagementServiceCmkArn() {
        return managementServiceCmkArn;
    }

    public void setManagementServiceCmkArn(String managementServiceCmkArn) {
        this.managementServiceCmkArn = managementServiceCmkArn;
    }
}
