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

package com.nike.cerberus.util;

import com.beust.jcommander.internal.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RandomStringGeneratorTest {

    @Test
    public void sanityTestGet() {

        RandomStringGenerator generator = new RandomStringGenerator();

        Set<String> previous = Sets.newHashSet();
        for (int i = 0; i < 1000; i++) {

            String current = generator.get();

            // expecting unique Strings
            assertFalse(previous.contains(current));

            // produces 30 or less characters
            assertTrue("failed on " + current, current.length() <= 30);

            // should never be empty
            assertFalse(current.isEmpty());

            previous.add(current);
        }
    }

}