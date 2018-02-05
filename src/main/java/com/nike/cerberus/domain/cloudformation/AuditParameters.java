package com.nike.cerberus.domain.cloudformation;

public class AuditParameters {
    private String cmsIamRoleArn;
    private String accountAdminArn;
    private String managementServiceCmkArn;

    public String getCmsIamRoleArn() {
        return cmsIamRoleArn;
    }

    public AuditParameters setCmsIamRoleArn(String cmsIamRoleArn) {
        this.cmsIamRoleArn = cmsIamRoleArn;
        return this;
    }

    public String getAccountAdminArn() {
        return accountAdminArn;
    }

    public AuditParameters setAccountAdminArn(String accountAdminArn) {
        this.accountAdminArn = accountAdminArn;
        return this;
    }

    public String getManagementServiceCmkArn() {
        return managementServiceCmkArn;
    }

    public AuditParameters setManagementServiceCmkArn(String managementServiceCmkArn) {
        this.managementServiceCmkArn = managementServiceCmkArn;
        return this;
    }
}
