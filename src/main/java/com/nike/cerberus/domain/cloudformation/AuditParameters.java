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

public class AuditParameters {

    private String cmsIamRoleArn;
    private String accountAdminArn;
    private String environmentName;

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public AuditParameters setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
        return this;
    }

    public String getAccountAdminArn() {
        return accountAdminArn;
    }

    public AuditParameters setAccountAdminArn(String accountAdminArn) {
        this.accountAdminArn = accountAdminArn;
        return this;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public AuditParameters setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }
}
