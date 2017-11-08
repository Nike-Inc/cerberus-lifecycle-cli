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

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Represents the database stack inputs.
 */
public class DatabaseParameters implements TagParameters {

    private String cmsDbInstanceAz1;

    private String cmsDbInstanceAz2;

    private String cmsDbInstanceAz3;

    private String cmsDbInstanceSize;

    private String cmsDbMasterPassword;

    private String cmsDbMasterUsername;

    private String cmsDbName;

    private String sgStackName;

    private String vpcSubnetIdForAz1;

    private String vpcSubnetIdForAz2;

    private String vpcSubnetIdForAz3;

    @JsonUnwrapped
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    public String getCmsDbInstanceAz1() {
        return cmsDbInstanceAz1;
    }

    public DatabaseParameters setCmsDbInstanceAz1(String cmsDbInstanceAz1) {
        this.cmsDbInstanceAz1 = cmsDbInstanceAz1;
        return this;
    }

    public String getCmsDbInstanceAz2() {
        return cmsDbInstanceAz2;
    }

    public DatabaseParameters setCmsDbInstanceAz2(String cmsDbInstanceAz2) {
        this.cmsDbInstanceAz2 = cmsDbInstanceAz2;
        return this;
    }

    public String getCmsDbInstanceAz3() {
        return cmsDbInstanceAz3;
    }

    public DatabaseParameters setCmsDbInstanceAz3(String cmsDbInstanceAz3) {
        this.cmsDbInstanceAz3 = cmsDbInstanceAz3;
        return this;
    }

    public String getCmsDbInstanceSize() {
        return cmsDbInstanceSize;
    }

    public DatabaseParameters setCmsDbInstanceSize(String cmsDbInstanceSize) {
        this.cmsDbInstanceSize = cmsDbInstanceSize;
        return this;
    }

    public String getCmsDbMasterPassword() {
        return cmsDbMasterPassword;
    }

    public DatabaseParameters setCmsDbMasterPassword(String cmsDbMasterPassword) {
        this.cmsDbMasterPassword = cmsDbMasterPassword;
        return this;
    }

    public String getCmsDbMasterUsername() {
        return cmsDbMasterUsername;
    }

    public DatabaseParameters setCmsDbMasterUsername(String cmsDbMasterUsername) {
        this.cmsDbMasterUsername = cmsDbMasterUsername;
        return this;
    }

    public String getCmsDbName() {
        return cmsDbName;
    }

    public DatabaseParameters setCmsDbName(String cmsDbName) {
        this.cmsDbName = cmsDbName;
        return this;
    }

    public String getSgStackName() {
        return sgStackName;
    }

    public DatabaseParameters setSgStackName(String sgStackName) {
        this.sgStackName = sgStackName;
        return this;
    }

    public String getVpcSubnetIdForAz1() {
        return vpcSubnetIdForAz1;
    }

    public DatabaseParameters setVpcSubnetIdForAz1(String vpcSubnetIdForAz1) {
        this.vpcSubnetIdForAz1 = vpcSubnetIdForAz1;
        return this;
    }

    public String getVpcSubnetIdForAz2() {
        return vpcSubnetIdForAz2;
    }

    public DatabaseParameters setVpcSubnetIdForAz2(String vpcSubnetIdForAz2) {
        this.vpcSubnetIdForAz2 = vpcSubnetIdForAz2;
        return this;
    }

    public String getVpcSubnetIdForAz3() {
        return vpcSubnetIdForAz3;
    }

    public DatabaseParameters setVpcSubnetIdForAz3(String vpcSubnetIdForAz3) {
        this.vpcSubnetIdForAz3 = vpcSubnetIdForAz3;
        return this;
    }

    @Override
    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public DatabaseParameters setTagParameters(TagParametersDelegate tagParameters) {
        this.tagParameters = tagParameters;
        return this;
    }
}
