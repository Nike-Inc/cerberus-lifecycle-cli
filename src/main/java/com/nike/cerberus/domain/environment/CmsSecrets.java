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

package com.nike.cerberus.domain.environment;

/**
 * Represents sensitive information used by CMS.
 */
public class CmsSecrets {

    private String adminGroup;

    private String databasePassword;

    private String vaultToken;

    public String getAdminGroup() {
        return adminGroup;
    }

    public CmsSecrets setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
        return this;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public CmsSecrets setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
        return this;
    }

    public String getVaultToken() {
        return vaultToken;
    }

    public CmsSecrets setVaultToken(String vaultToken) {
        this.vaultToken = vaultToken;
        return this;
    }
}
