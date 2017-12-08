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

package com.nike.cerberus.domain.input;

import java.util.Optional;

/**
 * Stores all the region specific configuration for a given region
 */
public class RegionSpecificConfigurationInput {

    private boolean primary = false;
    private RdsRegionSpecificInput rds;
    private ManagementServiceRegionSpecificInput managementService;
    private String loadBalancerDomainNameOverride;

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Optional<RdsRegionSpecificInput> getRds() {
        return Optional.ofNullable(rds);
    }

    public void setRds(RdsRegionSpecificInput rds) {
        this.rds = rds;
    }

    public Optional<ManagementServiceRegionSpecificInput> getManagementService() {
        return Optional.ofNullable(managementService);
    }

    public void setManagementService(ManagementServiceRegionSpecificInput managementService) {
        this.managementService = managementService;
    }

    public Optional<String> getLoadBalancerDomainNameOverride() {
        return Optional.ofNullable(loadBalancerDomainNameOverride);
    }

    public void setLoadBalancerDomainNameOverride(String loadBalancerDomainNameOverride) {
        this.loadBalancerDomainNameOverride = loadBalancerDomainNameOverride;
    }
}
