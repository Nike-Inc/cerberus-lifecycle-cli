package com.nike.cerberus.domain.template;

/**
 * Consul configuration input.
 */
public class ConsulConfigurationInput {

    private String aclMasterToken;

    private String gossipEncryptionToken;

    private String datacenter;

    public String getAclMasterToken() {
        return aclMasterToken;
    }

    public ConsulConfigurationInput setAclMasterToken(String aclMasterToken) {
        this.aclMasterToken = aclMasterToken;
        return this;
    }

    public String getGossipEncryptionToken() {
        return gossipEncryptionToken;
    }

    public ConsulConfigurationInput setGossipEncryptionToken(String gossipEncryptionToken) {
        this.gossipEncryptionToken = gossipEncryptionToken;
        return this;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public ConsulConfigurationInput setDatacenter(String datacenter) {
        this.datacenter = datacenter;
        return this;
    }
}
