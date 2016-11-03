package com.nike.cerberus.domain.vault;

/**
 * Supported backends for audit in Vault.
 */
public enum AuditBackend {
    FILE("file"),
    SYSLOG("syslog");

    private final String type;

    AuditBackend(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
