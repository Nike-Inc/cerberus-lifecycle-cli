package com.nike.cerberus.domain.configuration;

/**
 * POJO creating the ACL token and entry JSON.
 */
public class VaultAclEntry {

    private String aclToken;

    private String entry;

    public String getAclToken() {
        return aclToken;
    }

    public VaultAclEntry setAclToken(String aclToken) {
        this.aclToken = aclToken;
        return this;
    }

    public String getEntry() {
        return entry;
    }

    public VaultAclEntry setEntry(String entry) {
        this.entry = entry;
        return this;
    }
}
