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

package com.nike.cerberus.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Convert objects to {@code Map<String,String>} to pass to CloudFormation
 */
@Singleton
public class CloudFormationObjectMapper {

    private final ObjectMapper om;

    public CloudFormationObjectMapper() {
        om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    }

    public Map<String, String> convertValue(Object object) {
        return om.convertValue(
                object,
                new TypeReference<Map<String, String>>() {
                }
        );
    }

    public <T> T convertValue(Map<String, String> stackOutputs, Class<T> outputClass) {
        return om.convertValue(stackOutputs, outputClass);
    }
}
