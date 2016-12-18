package com.nike.cerberus.domain.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CerberusStack {

    private String certPath;
    private String amiId;
    private String instanceSize;
    private String keyPairName;

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getAmiId() {
        return amiId;
    }

    public void setAmiId(String amiId) {
        this.amiId = amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public void setInstanceSize(String instanceSize) {
        this.instanceSize = instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public void setKeyPairName(String keyPairName) {
        this.keyPairName = keyPairName;
    }
}
