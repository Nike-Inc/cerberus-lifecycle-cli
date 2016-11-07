/*
 * Copyright (c) 2016 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
