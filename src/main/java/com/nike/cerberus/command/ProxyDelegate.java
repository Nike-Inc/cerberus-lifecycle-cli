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

package com.nike.cerberus.command;

import com.beust.jcommander.Parameter;

import java.net.Proxy;

/**
 * Represents proxy parameters since some commands require making connections that may need to go through a proxy.
 */
public class ProxyDelegate {

    @Parameter(names = {"--proxy-type"}, description = "Type of proxy.")
    private Proxy.Type proxyType = Proxy.Type.DIRECT;

    @Parameter(names = {"--proxy-host"}, description = "Proxy host.")
    private String proxyHost;

    @Parameter(names = {"--proxy-port"}, description = "Proxy port.")
    private Integer proxyPort;

    public Proxy.Type getProxyType() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }
}
