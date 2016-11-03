package com.nike.cerberus.domain.environment;

/**
 * Represents sensitive information used by CMS.
 */
public class CmsSecrets {

    private String adminGroup;

    private String databasePassword;

    private String vaultToken;

    public String getAdminGroup() {
        return adminGroup;
    }

    public CmsSecrets setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
        return this;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public CmsSecrets setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
        return this;
    }

    public String getVaultToken() {
        return vaultToken;
    }

    public CmsSecrets setVaultToken(String vaultToken) {
        this.vaultToken = vaultToken;
        return this;
    }
}
