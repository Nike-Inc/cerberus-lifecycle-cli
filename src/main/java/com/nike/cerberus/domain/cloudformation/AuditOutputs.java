package com.nike.cerberus.domain.cloudformation;

public class AuditOutputs {
    String auditBucketName;

    public String getAuditBucketName() {
        return auditBucketName;
    }

    public AuditOutputs setAuditBucketName(String auditBucketName) {
        this.auditBucketName = auditBucketName;
        return this;
    }
}
