package com.nike.cerberus.util;

import com.beust.jcommander.IStringConverter;
import com.nike.cerberus.domain.environment.Stack;

/**
 * JCommander converter
 */
public class StackConverter implements IStringConverter<Stack> {

    @Override
    public Stack convert(String value) {
        return Stack.fromName(value);
    }
}