package com.nike.cerberus.domain.template;

/**
 * Input for the template generation of the Vault ACL entry.
 */
public class VaultAclInput {

    private String aclToken;

    public String getAclToken() {
        return aclToken;
    }

    public VaultAclInput setAclToken(String aclToken) {
        this.aclToken = aclToken;
        return this;
    }
}
