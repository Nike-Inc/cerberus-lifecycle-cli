package com.nike.cerberus.domain.environment;

/**
 * Container for sensitive data needed by Cerberus.
 */
public class Secrets {

    private ConsulSecrets consul = new ConsulSecrets();

    private CmsSecrets cms = new CmsSecrets();

    private VaultSecrets vault = new VaultSecrets();

    public ConsulSecrets getConsul() {
        return consul;
    }

    public Secrets setConsul(ConsulSecrets consul) {
        this.consul = consul;
        return this;
    }

    public CmsSecrets getCms() {
        return cms;
    }

    public Secrets setCms(CmsSecrets cms) {
        this.cms = cms;
        return this;
    }

    public VaultSecrets getVault() {
        return vault;
    }

    public Secrets setVault(VaultSecrets vault) {
        this.vault = vault;
        return this;
    }
}
