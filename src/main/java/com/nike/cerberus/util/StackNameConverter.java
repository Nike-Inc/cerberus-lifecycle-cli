package com.nike.cerberus.util;

import com.beust.jcommander.IStringConverter;
import com.nike.cerberus.domain.environment.Stack;

public class StackNameConverter implements IStringConverter<Stack> {

    @Override
    public Stack convert(String value) {
        return Stack.fromName(value);
    }
}