/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.utils;

public class TestUtils {

    private TestUtils() {
        // no constructing
    }

    public static String getRequiredEnvVar(String key, String msg) {
        String value = System.getenv(key);
        if (value == null || "" == value.trim()) {
            throw new IllegalStateException(String.format("The environment variable: %s is required for these tests, msg: %s", key, msg));
        }
        return value;
    }

}
