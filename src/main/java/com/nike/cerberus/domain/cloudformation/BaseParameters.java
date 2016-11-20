/*
 * Copyright (c) 2016 Nike, Inc.
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

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Represents the base stack inputs.
 */
public class BaseParameters implements TagParameters {

    private String accountAdminArn;

    private String az1;

    private String az2;

    private String az3;

    private String cmsDbAllocatedStorage;

    private String cmsDbInstanceSize;

    private String cmsDbName;

    private String cmsDbMasterUsername;

    private String cmsDbMasterPassword;

    private Integer cmsDbPort;

    private String vpcHostedZoneName;

    @JsonUnwrapped
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getAccountAdminArn() {
        return accountAdminArn;
    }

    public BaseParameters setAccountAdminArn(String accountAdminArn) {
        this.accountAdminArn = accountAdminArn;
        return this;
    }

    public String getAz1() {
        return az1;
    }

    public BaseParameters setAz1(String az1) {
        this.az1 = az1;
        return this;
    }

    public String getAz2() {
        return az2;
    }

    public BaseParameters setAz2(String az2) {
        this.az2 = az2;
        return this;
    }

    public String getAz3() {
        return az3;
    }

    public BaseParameters setAz3(String az3) {
        this.az3 = az3;
        return this;
    }

    public String getCmsDbAllocatedStorage() {
        return cmsDbAllocatedStorage;
    }

    public BaseParameters setCmsDbAllocatedStorage(String cmsDbAllocatedStorage) {
        this.cmsDbAllocatedStorage = cmsDbAllocatedStorage;
        return this;
    }

    public String getCmsDbInstanceSize() {
        return cmsDbInstanceSize;
    }

    public BaseParameters setCmsDbInstanceSize(String cmsDbInstanceSize) {
        this.cmsDbInstanceSize = cmsDbInstanceSize;
        return this;
    }

    public String getCmsDbName() {
        return cmsDbName;
    }

    public BaseParameters setCmsDbName(String cmsDbName) {
        this.cmsDbName = cmsDbName;
        return this;
    }

    public String getCmsDbMasterUsername() {
        return cmsDbMasterUsername;
    }

    public BaseParameters setCmsDbMasterUsername(String cmsDbMasterUsername) {
        this.cmsDbMasterUsername = cmsDbMasterUsername;
        return this;
    }

    public String getCmsDbMasterPassword() {
        return cmsDbMasterPassword;
    }

    public BaseParameters setCmsDbMasterPassword(String cmsDbMasterPassword) {
        this.cmsDbMasterPassword = cmsDbMasterPassword;
        return this;
    }

    public Integer getCmsDbPort() {
        return cmsDbPort;
    }

    public BaseParameters setCmsDbPort(Integer cmsDbPort) {
        this.cmsDbPort = cmsDbPort;
        return this;
    }

    public String getVpcHostedZoneName() {
        return vpcHostedZoneName;
    }

    public BaseParameters setVpcHostedZoneName(String vpcHostedZoneName) {
        this.vpcHostedZoneName = vpcHostedZoneName;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public BaseParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }
}
