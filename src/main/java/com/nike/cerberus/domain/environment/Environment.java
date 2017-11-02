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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * General purpose environment data that isn't sensitive.
 */
public class Environment {

    private String az1;

    private String az2;

    private String az3;

    private Map<StackName, String> stackMap;

    private Map<StackName, String> serverCertificateIdMap;

    private String configKeyId;

    private Map<String, BackupRegionInfo> regionBackupBucketMap;

    private Set<String> backupAdminIamPrincipals;

    private String metricsTopicArn;

    /**
     * Is the environment configured for continuous delivery
     */
    private boolean isCd;

    public Environment() {
        stackMap = new HashMap<>();
        for (StackName stackName : StackName.values()) {
            stackMap.put(stackName, "");
        }

        serverCertificateIdMap = new HashMap<>();
        serverCertificateIdMap.put(StackName.CMS, "");
    }

    public String getAz1() {
        return az1;
    }

    public Environment setAz1(String az1) {
        this.az1 = az1;
        return this;
    }

    public String getAz2() {
        return az2;
    }

    public Environment setAz2(String az2) {
        this.az2 = az2;
        return this;
    }

    public String getAz3() {
        return az3;
    }

    public Environment setAz3(String az3) {
        this.az3 = az3;
        return this;
    }

    public Map<StackName, String> getStackMap() {
        return stackMap;
    }

    public Environment setStackMap(Map<StackName, String> stackMap) {
        this.stackMap = stackMap;
        return this;
    }

    public Map<StackName, String> getServerCertificateIdMap() {
        return serverCertificateIdMap;
    }

    public Environment setServerCertificateIdMap(Map<StackName, String> serverCertificateIdMap) {
        this.serverCertificateIdMap = serverCertificateIdMap;
        return this;
    }

    public String getConfigKeyId() {
        return configKeyId;
    }

    public Environment setConfigKeyId(String configKeyId) {
        this.configKeyId = configKeyId;
        return this;
    }

    public boolean isCd() {
        return isCd;
    }

    public Environment setCd(boolean cd) {
        isCd = cd;
        return this;
    }

    public Map<String, BackupRegionInfo> getRegionBackupBucketMap() {
        return regionBackupBucketMap == null ? new HashMap<>() : regionBackupBucketMap;
    }

    public void setRegionBackupBucketMap(Map<String, BackupRegionInfo> regionBackupBucketMap) {
        this.regionBackupBucketMap = regionBackupBucketMap;
    }

    public Set<String> getBackupAdminIamPrincipals() {
        return backupAdminIamPrincipals == null ? new HashSet<>() : backupAdminIamPrincipals;
    }

    public void setBackupAdminIamPrincipals(Set<String> backupAdminIamPrincipals) {
        this.backupAdminIamPrincipals = backupAdminIamPrincipals;
    }

    public String getMetricsTopicArn() {
        return metricsTopicArn;
    }

    public void setMetricsTopicArn(String metricsTopicArn) {
        this.metricsTopicArn = metricsTopicArn;
    }
}
