/*
 * Copyright (c) 2020 Nike, Inc.
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
 * Represents the WAF logging stack inputs.
 */
public class WafLoggingParameters {
    private String environmentName;

    private String s3Prefix;

    public String getS3Prefix() {
        return s3Prefix;
    }

    public WafLoggingParameters setS3Prefix(String s3Prefix) {
        this.s3Prefix = s3Prefix;
        return this;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public WafLoggingParameters setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
        return this;
    }
}
