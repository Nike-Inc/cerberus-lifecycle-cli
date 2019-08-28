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

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

/**
 * Generate a key
 */
public class KeyGenerator {
    public static final String HMACSHA512 = "HmacSHA512";


    public SecretKey generateKey(String algorithm) {
        javax.crypto.KeyGenerator gen;
        try {
            gen = javax.crypto.KeyGenerator.getInstance(algorithm);
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The " + algorithm + " algorithm is not available.");
        }
    }
}
