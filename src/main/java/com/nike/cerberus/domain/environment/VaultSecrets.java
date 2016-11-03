package com.nike.cerberus.domain.environment;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents sensitive information used by Vault.
 */
public class VaultSecrets {

    private List<String> keys = new LinkedList<>();

    private String rootToken;

    public List<String> getKeys() {
        return keys;
    }

    public VaultSecrets setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }

    public String getRootToken() {
        return rootToken;
    }

    public VaultSecrets setRootToken(String rootToken) {
        this.rootToken = rootToken;
        return this;
    }
}
