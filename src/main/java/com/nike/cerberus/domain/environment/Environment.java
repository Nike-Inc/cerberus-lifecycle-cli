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

    private String domainName;

    private Map<String, String> stackMap;

    private Map<String, String> serverCertificateIdMap;

    private String configKeyId;

    private Map<String, BackupRegionInfo> regionBackupBucketMap;

    private Set<String> backupAdminIamPrincipals;

    private String metricsTopicArn;

    public Environment() {
        stackMap = new HashMap<>();
        for (Stack stack : Stack.ALL_STACKS) {
            stackMap.put(stack.getName(), "");
        }

        serverCertificateIdMap = new HashMap<>();
        serverCertificateIdMap.put(Stack.CMS.getName(), "");
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Map<String, String> getStackMap() {
        return stackMap;
    }

    public Map<String, String> getServerCertificateIdMap() {
        return serverCertificateIdMap;
    }

    public String getConfigKeyId() {
        return configKeyId;
    }

    public Environment setConfigKeyId(String configKeyId) {
        this.configKeyId = configKeyId;
        return this;
    }

    public Map<String, BackupRegionInfo> getRegionBackupBucketMap() {
        return regionBackupBucketMap == null ? new HashMap<>() : regionBackupBucketMap;
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
