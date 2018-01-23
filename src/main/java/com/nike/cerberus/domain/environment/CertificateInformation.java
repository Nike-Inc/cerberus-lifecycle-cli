/*
 * Copyright (c) 2017 Nike, Inc.
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

package com.nike.cerberus.domain.environment;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class CertificateInformation {

    private String certificateName;
    private String certificateId;
    private String identityManagementCertificateArn;
    private DateTime notBefore;
    private DateTime notAfter;
    private DateTime uploaded;
    private String commonName;
    private List<String> subjectAlternateNames;

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public DateTime getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(DateTime notBefore) {
        this.notBefore = notBefore;
    }

    public DateTime getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(DateTime notAfter) {
        this.notAfter = notAfter;
    }

    public DateTime getUploaded() {
        return uploaded;
    }

    public void setUploaded(DateTime uploaded) {
        this.uploaded = uploaded;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public List<String> getSubjectAlternateNames() {
        return subjectAlternateNames;
    }

    public void setSubjectAlternateNames(List<String> subjectAlternateNames) {
        this.subjectAlternateNames = subjectAlternateNames;
    }

    public String getIdentityManagementCertificateArn() {
        return identityManagementCertificateArn;
    }

    public void setIdentityManagementCertificateArn(String identityManagementCertificateArn) {
        this.identityManagementCertificateArn = identityManagementCertificateArn;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormat.fullDateTime();
        return "CertificateInformation{" +
                "certificateName='" + certificateName + '\'' +
                ", certificateId='" + certificateId + '\'' +
                ", identityManagementCertificateArn='" + identityManagementCertificateArn + '\'' +
                ", notBefore=" + fmt.print(notBefore) +
                ", notAfter=" + fmt.print(notAfter) +
                ", uploaded=" + fmt.print(uploaded) +
                ", commonName='" + commonName + '\'' +
                ", subjectAlternateNames=" + subjectAlternateNames +
                '}';
    }

    public static final class Builder {
        private String certificateName;
        private String certificateId;
        private String identityManagementCertificateArn;
        private DateTime notBefore;
        private DateTime notAfter;
        private DateTime uploaded;
        private String commonName;
        private List<String> subjectAlternateNames;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withCertificateName(String certificateName) {
            this.certificateName = certificateName;
            return this;
        }

        public Builder withCertificateId(String certificateId) {
            this.certificateId = certificateId;
            return this;
        }

        public Builder withIdentityManagementCertificateArn(String identityManagementCertificateArn) {
            this.identityManagementCertificateArn = identityManagementCertificateArn;
            return this;
        }

        public Builder withNotBefore(DateTime notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public Builder withNotAfter(DateTime notAfter) {
            this.notAfter = notAfter;
            return this;
        }

        public Builder withUploaded(DateTime uploaded) {
            this.uploaded = uploaded;
            return this;
        }

        public Builder withCommonName(String commonName) {
            this.commonName = commonName;
            return this;
        }

        public Builder withSubjectAlternateNames(List<String> subjectAlternateNames) {
            this.subjectAlternateNames = subjectAlternateNames;
            return this;
        }

        public CertificateInformation build() {
            CertificateInformation certificateInformation = new CertificateInformation();
            certificateInformation.setCertificateName(certificateName);
            certificateInformation.setCertificateId(certificateId);
            certificateInformation.setIdentityManagementCertificateArn(identityManagementCertificateArn);
            certificateInformation.setNotBefore(notBefore);
            certificateInformation.setNotAfter(notAfter);
            certificateInformation.setUploaded(uploaded);
            certificateInformation.setCommonName(commonName);
            certificateInformation.setSubjectAlternateNames(subjectAlternateNames);
            return certificateInformation;
        }
    }
}
