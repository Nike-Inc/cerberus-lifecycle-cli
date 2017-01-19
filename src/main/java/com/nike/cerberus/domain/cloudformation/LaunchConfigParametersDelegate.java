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
 * Common parameters for stack components.
 */
public class LaunchConfigParametersDelegate {

    private String amiId;

    private String instanceSize;

    private String keyPairName;

    private String userData;

    private int desiredInstances;

    private int maximumInstances;

    private int minimumInstances;

    public String getAmiId() {
        return amiId;
    }

    public LaunchConfigParametersDelegate setAmiId(String amiId) {
        this.amiId = amiId;
        return this;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public LaunchConfigParametersDelegate setInstanceSize(String instanceSize) {
        this.instanceSize = instanceSize;
        return this;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public LaunchConfigParametersDelegate setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
        return this;
    }

    public String getUserData() {
        return userData;
    }

    public LaunchConfigParametersDelegate setUserData(String userData) {
        this.userData = userData;
        return this;
    }

    public int getDesiredInstances() {
        return desiredInstances;
    }

    public LaunchConfigParametersDelegate setDesiredInstances(int desiredInstances) {
        this.desiredInstances = desiredInstances;
        return this;
    }

    public int getMaximumInstances() {
        return maximumInstances;
    }

    public LaunchConfigParametersDelegate setMaximumInstances(int maximumInstances) {
        this.maximumInstances = maximumInstances;
        return this;
    }

    public int getMinimumInstances() {
        return minimumInstances;
    }

    public LaunchConfigParametersDelegate setMinimumInstances(int minimumInstances) {
        this.minimumInstances = minimumInstances;
        return this;
    }
}
