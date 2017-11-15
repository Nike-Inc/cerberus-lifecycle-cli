package com.nike.cerberus.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MapOfStringsTypeRefTest {

    @Test
    public void sanityTest() {

        final ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

        BaseParameters parameters = new BaseParameters();
        parameters.setAccountAdminArn("fake");

        Map<String, String> result = om.convertValue(parameters, new MapOfStringsTypeRef());

        assertEquals("fake", result.get("accountAdminArn"));
    }
}