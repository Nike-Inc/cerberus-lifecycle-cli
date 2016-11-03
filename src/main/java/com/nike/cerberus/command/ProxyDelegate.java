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
