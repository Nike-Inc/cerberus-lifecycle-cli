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

package com.nike.cerberus.domain.cloudformation;

public class BaseOutputs {

    private String configBucketDomainName;

    private String configBucketName;

    private String environmentDataKmsCmkArn;

    private String cmsSecureDataKmsCmkArn;

    public String getConfigBucketDomainName() {
        return configBucketDomainName;
    }

    public BaseOutputs setConfigBucketDomainName(String configBucketDomainName) {
        this.configBucketDomainName = configBucketDomainName;
        return this;
    }

    public String getConfigBucketName() {
        return configBucketName;
    }

    public BaseOutputs setConfigBucketName(String configBucketName) {
        this.configBucketName = configBucketName;
        return this;
    }

    public String getEnvironmentDataKmsCmkArn() {
        return environmentDataKmsCmkArn;
    }

    public void setEnvironmentDataKmsCmkArn(String environmentDataKmsCmkArn) {
        this.environmentDataKmsCmkArn = environmentDataKmsCmkArn;
    }

    public String getCmsSecureDataKmsCmkArn() {
        return cmsSecureDataKmsCmkArn;
    }

    public void setCmsSecureDataKmsCmkArn(String cmsSecureDataKmsCmkArn) {
        this.cmsSecureDataKmsCmkArn = cmsSecureDataKmsCmkArn;
    }
}
