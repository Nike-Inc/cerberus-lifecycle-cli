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
