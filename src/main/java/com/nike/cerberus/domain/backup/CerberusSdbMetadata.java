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

import java.util.HashSet;
import java.util.Set;

/**
 * Simple pojo for wrapping Cerberus SDB metadata.
 */
public class CerberusSdbMetadata {

    private Integer numberOfKeyValuePairs = 0;
    private Integer numberOfDataNodes = 0;
    private Set<String> uniqueOwnerGroups = new HashSet<>();
    private Set<String> uniqueIamRoles = new HashSet<>();
    private Set<String> uniqueNonOwnerGroups = new HashSet<>();

    public CerberusSdbMetadata() {
    }

    public CerberusSdbMetadata(Integer numberOfKeyValuePairs,
                               Integer numberOfDataNodes,
                               Set<String> uniqueOwnerGroups,
                               Set<String> uniqueIamRoles,
                               Set<String> uniqueNonOwnerGroups) {

        this.numberOfKeyValuePairs = numberOfKeyValuePairs;
        this.numberOfDataNodes = numberOfDataNodes;
        this.uniqueOwnerGroups = uniqueOwnerGroups;
        this.uniqueIamRoles = uniqueIamRoles;
        this.uniqueNonOwnerGroups = uniqueNonOwnerGroups;
    }

    public Integer getNumberOfKeyValuePairs() {
        return numberOfKeyValuePairs;
    }

    public void setNumberOfKeyValuePairs(Integer numberOfKeyValuePairs) {
        this.numberOfKeyValuePairs = numberOfKeyValuePairs;
    }

    public Integer getNumberOfDataNodes() {
        return numberOfDataNodes;
    }

    public void setNumberOfDataNodes(Integer numberOfDataNodes) {
        this.numberOfDataNodes = numberOfDataNodes;
    }

    public Set<String> getUniqueOwnerGroups() {
        return uniqueOwnerGroups;
    }

    public void setUniqueOwnerGroups(Set<String> uniqueOwnerGroups) {
        this.uniqueOwnerGroups = uniqueOwnerGroups;
    }

    public Set<String> getUniqueIamRoles() {
        return uniqueIamRoles;
    }

    public void setUniqueIamRoles(Set<String> uniqueIamRoles) {
        this.uniqueIamRoles = uniqueIamRoles;
    }

    public Set<String> getUniqueNonOwnerGroups() {
        return uniqueNonOwnerGroups;
    }

    public void setUniqueNonOwnerGroups(Set<String> uniqueNonOwnerGroups) {
        this.uniqueNonOwnerGroups = uniqueNonOwnerGroups;
    }
}
