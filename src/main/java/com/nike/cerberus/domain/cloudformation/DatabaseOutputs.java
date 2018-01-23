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
 * Represents the database stack outputs.
 */
public class DatabaseOutputs {

    private String cmsDbAddress;

    private String cmsDbInstanceId1;

    private String cmsDbInstanceId2;

    private String cmsDbJdbcConnectionString;

    public String getCmsDbAddress() {
        return cmsDbAddress;
    }

    public DatabaseOutputs setCmsDbAddress(String cmsDbAddress) {
        this.cmsDbAddress = cmsDbAddress;
        return this;
    }

    public String getCmsDbInstanceId1() {
        return cmsDbInstanceId1;
    }

    public DatabaseOutputs setCmsDbInstanceId1(String cmsDbInstanceId1) {
        this.cmsDbInstanceId1 = cmsDbInstanceId1;
        return this;
    }

    public String getCmsDbInstanceId2() {
        return cmsDbInstanceId2;
    }

    public DatabaseOutputs setCmsDbInstanceId2(String cmsDbInstanceId2) {
        this.cmsDbInstanceId2 = cmsDbInstanceId2;
        return this;
    }

    public String getCmsDbJdbcConnectionString() {
        return cmsDbJdbcConnectionString;
    }

    public DatabaseOutputs setCmsDbJdbcConnectionString(String cmsDbJdbcConnectionString) {
        this.cmsDbJdbcConnectionString = cmsDbJdbcConnectionString;
        return this;
    }
}
