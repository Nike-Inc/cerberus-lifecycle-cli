/*
 * Copyright (c) 2016 Nike Inc.
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

package com.nike.cerberus.domain.template;

/**
 * POJO for gateway configuration inputs.
 */
public class GatewayConfigurationInput {

    private String gatewayHost;

    private String dashboardHost;

    private String vaultHost;

    private String cmsHost;

    public String getGatewayHost() {
        return gatewayHost;
    }

    public GatewayConfigurationInput setGatewayHost(String gatewayHost) {
        this.gatewayHost = gatewayHost;
        return this;
    }

    public String getDashboardHost() {
        return dashboardHost;
    }

    public GatewayConfigurationInput setDashboardHost(String dashboardHost) {
        this.dashboardHost = dashboardHost;
        return this;
    }

    public String getVaultHost() {
        return vaultHost;
    }

    public GatewayConfigurationInput setVaultHost(String vaultHost) {
        this.vaultHost = vaultHost;
        return this;
    }

    public String getCmsHost() {
        return cmsHost;
    }

    public GatewayConfigurationInput setCmsHost(String cmsHost) {
        this.cmsHost = cmsHost;
        return this;
    }
}
