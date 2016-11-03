package com.nike.cerberus.domain.cloudformation;

import java.util.List;
import java.util.Map;

/**
 * POJO for representing a CloudFormation stack parameter and outputs.
 */
public class StackInfo {

    private String stackId;

    private Map<String, String> stackParameters;

    private Map<String, String> stackOutputs;

    private List<String> publicDnsForInstances;

    public String getStackId() {
        return stackId;
    }

    public StackInfo setStackId(String stackId) {
        this.stackId = stackId;
        return this;
    }

    public Map<String, String> getStackParameters() {
        return stackParameters;
    }

    public StackInfo setStackParameters(Map<String, String> stackParameters) {
        this.stackParameters = stackParameters;
        return this;
    }

    public Map<String, String> getStackOutputs() {
        return stackOutputs;
    }

    public StackInfo setStackOutputs(Map<String, String> stackOutputs) {
        this.stackOutputs = stackOutputs;
        return this;
    }

    public List<String> getPublicDnsForInstances() {
        return publicDnsForInstances;
    }

    public StackInfo setPublicDnsForInstances(List<String> publicDnsForInstances) {
        this.publicDnsForInstances = publicDnsForInstances;
        return this;
    }
}
