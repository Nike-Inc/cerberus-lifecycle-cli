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

package com.nike.cerberus.domain.environment;

import java.util.Optional;

public class RegionData {

    private boolean primary;
    private String configBucket;
    private String environmentDataKmsCmkArn;
    private String cmsSecureDataKmsCmkArn;

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Optional<String> getConfigBucket() {
        return Optional.ofNullable(configBucket);
    }

    public void setConfigBucket(String configBucket) {
        this.configBucket = configBucket;
    }

    public Optional<String> getEnvironmentDataKmsCmkArn() {
        return Optional.ofNullable(environmentDataKmsCmkArn);
    }

    public void setEnvironmentDataKmsCmkArn(String environmentDataKmsCmkArn) {
        this.environmentDataKmsCmkArn = environmentDataKmsCmkArn;
    }

    public Optional<String> getCmsSecureDataKmsCmkArn() {
        return Optional.ofNullable(cmsSecureDataKmsCmkArn);
    }

    public void setCmsSecureDataKmsCmkArn(String cmsSecureDataKmsCmkArn) {
        this.cmsSecureDataKmsCmkArn = cmsSecureDataKmsCmkArn;
    }

}
