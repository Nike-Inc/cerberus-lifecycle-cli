package com.nike.cerberus.domain.environment;

/**
 * Represents sensitive information used by Consul.
 */
public class ConsulSecrets {

    private String gossipEncryptionToken;

    private String aclMasterToken;

    private String vaultAclToken;

    public String getGossipEncryptionToken() {
        return gossipEncryptionToken;
    }

    public ConsulSecrets setGossipEncryptionToken(String gossipEncryptionToken) {
        this.gossipEncryptionToken = gossipEncryptionToken;
        return this;
    }

    public String getAclMasterToken() {
        return aclMasterToken;
    }

    public ConsulSecrets setAclMasterToken(String aclMasterToken) {
        this.aclMasterToken = aclMasterToken;
        return this;
    }

    public String getVaultAclToken() {
        return vaultAclToken;
    }

    public ConsulSecrets setVaultAclToken(String vaultAclToken) {
        this.vaultAclToken = vaultAclToken;
        return this;
    }
}
