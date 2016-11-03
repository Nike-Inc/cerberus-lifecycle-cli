package com.nike.cerberus.domain.cloudformation;

/**
 * Common parameters for stack components.
 */
public class LaunchConfigParametersDelegate {

    private String amiId;

    private String instanceSize;

    private String keyPairName;

    private String userData;

    public String getAmiId() {
        return amiId;
    }

    public LaunchConfigParametersDelegate setAmiId(String amiId) {
        this.amiId = amiId;
        return this;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public LaunchConfigParametersDelegate setInstanceSize(String instanceSize) {
        this.instanceSize = instanceSize;
        return this;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public LaunchConfigParametersDelegate setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
        return this;
    }

    public String getUserData() {
        return userData;
    }

    public LaunchConfigParametersDelegate setUserData(String userData) {
        this.userData = userData;
        return this;
    }
}
