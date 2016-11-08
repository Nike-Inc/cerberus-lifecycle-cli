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

package com.nike.cerberus.domain.configuration;

import com.nike.cerberus.domain.template.ConsulConfigurationInput;

/**
 * Consul configuration POJO.
 */
public class ConsulConfiguration {

    private ConsulConfigurationInput input;

    private String clientConfiguration;

    private String serverConfiguration;

    public ConsulConfigurationInput getInput() {
        return input;
    }

    public ConsulConfiguration setInput(ConsulConfigurationInput input) {
        this.input = input;
        return this;
    }

    public String getClientConfiguration() {
        return clientConfiguration;
    }

    public ConsulConfiguration setClientConfiguration(String clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        return this;
    }

    public String getServerConfiguration() {
        return serverConfiguration;
    }

    public ConsulConfiguration setServerConfiguration(String serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
        return this;
    }
}
