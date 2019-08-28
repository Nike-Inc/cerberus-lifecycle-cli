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

import org.junit.Test;

import javax.crypto.SecretKey;
import java.util.Arrays;

import static org.junit.Assert.*;

public class KeyGeneratorTest {

    @Test
    public void testGenerateKey() {
        KeyGenerator generator = new KeyGenerator();
        SecretKey secretKey1 = generator.generateKey(KeyGenerator.HMACSHA512);
        SecretKey secretKey2 = generator.generateKey(KeyGenerator.HMACSHA512);
        assertEquals("HmacSHA512", secretKey1.getAlgorithm());
        assertEquals(512 / 8, secretKey1.getEncoded().length);
        assertFalse(Arrays.equals(secretKey1.getEncoded(), secretKey2.getEncoded()));
    }

    @Test(expected = IllegalStateException.class)
    public void testBogusAlgorithm() {
        KeyGenerator generator = new KeyGenerator();
        generator.generateKey("sleep sort");
    }
}
