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

package com.nike.cerberus.domain.input;

/**
 * Stores region specific Management Service parameters parsed from YAML
 */
public class ManagementServiceRegionSpecificInput {

    private String amiId;
    private String instanceSize;
    private String keyPairName;
    private String desiredInstances;
    private String maxInstances;
    private String minInstances;

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public void setInstanceSize(String instanceSize) {
        this.instanceSize = instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public void setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
    }

    public String getDesiredInstances() {
        return desiredInstances;
    }

    public void setDesiredInstances(String desiredInstances) {
        this.desiredInstances = desiredInstances;
    }

    public String getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(String maxInstances) {
        this.maxInstances = maxInstances;
    }

    public String getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(String minInstances) {
        this.minInstances = minInstances;
    }

}
