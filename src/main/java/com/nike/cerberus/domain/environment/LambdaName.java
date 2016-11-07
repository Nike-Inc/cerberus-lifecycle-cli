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

package com.nike.cerberus.domain.environment;

/**
 * Describes the lambdas that are part of Cerberus.
 */
public enum LambdaName {
    WAF("waf", "lambda/waf.jar"),
    CLOUD_FRONT_SG_GROUP_IP_SYNC("cf-sg-ip-sync", "lambda/cf-sg-ip-sync.zip");

    private final String name;

    private final String bucketKey;

    LambdaName(final String name, final String bucketKey) {
        this.name = name;
        this.bucketKey = bucketKey;
    }

    public String getName() {
        return name;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public static LambdaName fromName(final String name) {
        for (LambdaName lambdaName : LambdaName.values()) {
            if (lambdaName.getName().equals(name)) {
                return lambdaName;
            }
        }

        throw new IllegalArgumentException("Unknown lambda name: " + name);
    }
}
