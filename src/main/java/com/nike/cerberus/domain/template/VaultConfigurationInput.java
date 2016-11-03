package com.nike.cerberus.domain.template;

/**
 * Represents the inputs for the Vault configuration template.
 */
public class VaultConfigurationInput {

    private String datacenter;

    private String aclToken;

    public String getDatacenter() {
        return datacenter;
    }

    public VaultConfigurationInput setDatacenter(String datacenter) {
        this.datacenter = datacenter;
        return this;
    }

    public String getAclToken() {
        return aclToken;
    }

    public VaultConfigurationInput setAclToken(String aclToken) {
        this.aclToken = aclToken;
        return this;
    }
}
