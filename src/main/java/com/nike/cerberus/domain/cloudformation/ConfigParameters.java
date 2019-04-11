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
 * Represents the base stack inputs.
 */
public class ConfigParameters {

    private String accountAdminArn;

    private String cmsIamRoleArn;

    private String environmentName;

    public String getAccountAdminArn() {
        return accountAdminArn;
    }

    public ConfigParameters setAccountAdminArn(String accountAdminArn) {
        this.accountAdminArn = accountAdminArn;
        return this;
    }

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public ConfigParameters setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
        return this;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public ConfigParameters setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }
}
