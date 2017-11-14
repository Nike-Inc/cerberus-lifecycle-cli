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

package com.nike.cerberus.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.domain.environment.Stack;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class StackTest {

    @Test
    public void test_stack_serializes_as_expected() throws JsonProcessingException {
        Stack stack = Stack.CMS;

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(stack);

        assertEquals("\"cms\"", json);
    }

    @Test
    public void test_stack_deserializes_as_expected() throws IOException {
        Stack expected = Stack.CMS;

        ObjectMapper objectMapper = new ObjectMapper();
        Stack actual = objectMapper.readValue("\"cms\"", Stack.class);

        assertEquals(expected, actual);
    }
}
