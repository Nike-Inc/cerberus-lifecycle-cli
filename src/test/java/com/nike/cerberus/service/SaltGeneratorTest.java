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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SaltGeneratorTest {

    @Test
    public void sanityTestSalt() throws NoSuchAlgorithmException {
        SaltGenerator generator = new SaltGenerator();
        String s1 = generator.generateSalt();
        String s2 = generator.generateSalt();
        assertFalse(StringUtils.equals(s1, s2));
        assertEquals(88, s1.length());
        assertEquals(88, s2.length());
    }
}
