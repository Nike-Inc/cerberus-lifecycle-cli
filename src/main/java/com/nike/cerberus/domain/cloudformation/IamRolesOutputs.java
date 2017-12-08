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
 * Represents the base stack outputs.
 */
public class IamRolesOutputs {

    private String cmsIamRoleArn;

    private String cmsIamRoleName;

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public IamRolesOutputs setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
        return this;
    }

    public String getCmsIamRoleName() {
        return cmsIamRoleName;
    }

    public IamRolesOutputs setCmsIamRoleName(String cmsIamRoleName) {
        this.cmsIamRoleName = cmsIamRoleName;
        return this;
    }
}
