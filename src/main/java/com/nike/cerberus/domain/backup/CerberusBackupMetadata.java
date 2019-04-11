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

package com.nike.cerberus.domain.backup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Date;

/**
 * Simple pojo for wrapping Cerberus backup metadata.
 */
public class CerberusBackupMetadata {

    private String cerberusUrl;
    private Date backupDate;
    private Integer numberOfSdbs;
    private Integer numberOfDataNodes;
    private Integer numberOfKeyValuePairs;
    private Integer numberOfUniqueOwnerGroups;
    private Integer numberOfUniqueIamRoles;
    private Integer numberOfUniqueNonOwnerGroups;

    public String getCerberusUrl() {
        return cerberusUrl;
    }

    public CerberusBackupMetadata setCerberusUrl(String cerberusUrl) {
        this.cerberusUrl = cerberusUrl;
        return this;
    }

    @SuppressFBWarnings
    public Date getBackupDate() {
        return backupDate;
    }

    @SuppressFBWarnings
    public CerberusBackupMetadata setBackupDate(Date backupDate) {
        this.backupDate = backupDate;
        return this;
    }

    public Integer getNumberOfSdbs() {
        return numberOfSdbs;
    }

    public CerberusBackupMetadata setNumberOfSdbs(Integer numberOfSdbs) {
        this.numberOfSdbs = numberOfSdbs;
        return this;
    }

    public Integer getNumberOfDataNodes() {
        return numberOfDataNodes;
    }

    public CerberusBackupMetadata setNumberOfDataNodes(Integer numberOfDataNodes) {
        this.numberOfDataNodes = numberOfDataNodes;
        return this;
    }

    public Integer getNumberOfKeyValuePairs() {
        return numberOfKeyValuePairs;
    }

    public CerberusBackupMetadata setNumberOfKeyValuePairs(Integer numberOfKeyValuePairs) {
        this.numberOfKeyValuePairs = numberOfKeyValuePairs;
        return this;
    }

    public Integer getNumberOfUniqueOwnerGroups() {
        return numberOfUniqueOwnerGroups;
    }

    public CerberusBackupMetadata setNumberOfUniqueOwnerGroups(Integer numberOfUniqueOwnerGroups) {
        this.numberOfUniqueOwnerGroups = numberOfUniqueOwnerGroups;
        return this;
    }

    public Integer getNumberOfUniqueIamRoles() {
        return numberOfUniqueIamRoles;
    }

    public CerberusBackupMetadata setNumberOfUniqueIamRoles(Integer numberOfUniqueIamRoles) {
        this.numberOfUniqueIamRoles = numberOfUniqueIamRoles;
        return this;
    }

    public Integer getNumberOfUniqueNonOwnerGroups() {
        return numberOfUniqueNonOwnerGroups;
    }

    public CerberusBackupMetadata setNumberOfUniqueNonOwnerGroups(Integer numberOfUniqueNonOwnerGroups) {
        this.numberOfUniqueNonOwnerGroups = numberOfUniqueNonOwnerGroups;
        return this;
    }

    @Override
    public String toString() {
        return "CerberusBackupMetadata{" +
                "cerberusUrl='" + cerberusUrl + '\'' +
                ", backupDate=" + backupDate +
                ", numberOfSdbs=" + numberOfSdbs +
                ", numberOfDataNodes=" + numberOfDataNodes +
                ", numberOfKeyValuePairs=" + numberOfKeyValuePairs +
                ", numberOfUniqueOwnerGroups=" + numberOfUniqueOwnerGroups +
                ", numberOfUniqueIamRoles=" + numberOfUniqueIamRoles +
                ", numberOfUniqueNonOwnerGroups=" + numberOfUniqueNonOwnerGroups +
                '}';
    }
}
