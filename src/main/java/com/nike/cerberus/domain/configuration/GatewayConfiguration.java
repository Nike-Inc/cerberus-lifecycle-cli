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

package com.nike.cerberus.domain.configuration;

/**
 * Gateway configuration POJO.
 */
public class GatewayConfiguration {

    private String siteConfig;

    private String globalConfig;

    public String getSiteConfig() {
        return siteConfig;
    }

    public GatewayConfiguration setSiteConfig(String siteConfig) {
        this.siteConfig = siteConfig;
        return this;
    }

    public String getGlobalConfig() {
        return globalConfig;
    }

    public GatewayConfiguration setGlobalConfig(String globalConfig) {
        this.globalConfig = globalConfig;
        return this;
    }
}
