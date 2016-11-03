package com.nike.cerberus.domain.cloudformation;

/**
 * SSL config parameters
 */
public class SslConfigParametersDelegate {

    private String certPublicKey;

    private String sslCertificateArn;

    private String sslCertificateId;

    public String getCertPublicKey() {
        return certPublicKey;
    }

    public SslConfigParametersDelegate setCertPublicKey(String certPublicKey) {
        this.certPublicKey = certPublicKey;
        return this;
    }

    public String getSslCertificateArn() {
        return sslCertificateArn;
    }

    public SslConfigParametersDelegate setSslCertificateArn(String sslCertificateArn) {
        this.sslCertificateArn = sslCertificateArn;
        return this;
    }

    public String getSslCertificateId() {
        return sslCertificateId;
    }

    public SslConfigParametersDelegate setSslCertificateId(String sslCertificateId) {
        this.sslCertificateId = sslCertificateId;
        return this;
    }
}
