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

    public static final String PROXY_TYPE_LONG_ARG = "--proxy-type";
    public static final String PROXY_HOST_LONG_ARG = "--proxy-host";
    public static final String PROXY_PORT_LONG_ARG = "--proxy-port";
    @Parameter(names = {PROXY_TYPE_LONG_ARG}, description = "Type of proxy.")
    private Proxy.Type proxyType;

    @Parameter(names = {PROXY_HOST_LONG_ARG}, description = "Proxy host.")
    private String proxyHost;

    @Parameter(names = {PROXY_PORT_LONG_ARG}, description = "Proxy port.")
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

    public void setProxyType(Proxy.Type proxyType) {
        this.proxyType = proxyType;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
}
