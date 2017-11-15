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
import com.nike.cerberus.domain.environment.Environment;
import com.nike.cerberus.domain.environment.Stack;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StackTest {

    @Test
    public void test_that_stack_can_go_into_serialized_map() throws JsonProcessingException {
        Map<Stack, String> map = new HashMap<>();
        map.put(Stack.CMS, "foo");

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(map);

        assertEquals("{\"cms\":\"foo\"}", json);
    }

    @Test
    public void test_that_stack_can_deserialize_from_map() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Environment environment = new Environment();
        environment.getServerCertificateIdMap().put(Stack.CMS, "foo");
        String json = objectMapper.writeValueAsString(environment);
        Environment actual = objectMapper.readValue(json, Environment.class);

        assertTrue(actual.getServerCertificateIdMap().size() == 1);
    }

}
