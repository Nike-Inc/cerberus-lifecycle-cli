package com.nike.cerberus.util;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * This class can be used with an ObjectMapper to convert beans to {@code Map<String,String>}
 * <p>
 * For example, CloudFormation requires a simple {@code Map<String,String>} for parameters.
 */
public class MapOfStringsTypeRef extends TypeReference<Map<String, String>> {
    // implementation intentionally empty
}
