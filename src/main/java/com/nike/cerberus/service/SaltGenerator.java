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

package com.nike.cerberus.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * Generate a Salt
 */
public class SaltGenerator {

    private Random random = SecureRandom.getInstanceStrong();

    public SaltGenerator() throws NoSuchAlgorithmException {
    }

    /**
     * Generate a Salt as a Base64 encoded String we can store in a properties file
     */
    public String generateSalt() {
        byte[] salt = new byte[64];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
