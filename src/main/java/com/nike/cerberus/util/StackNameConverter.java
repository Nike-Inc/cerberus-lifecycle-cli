package com.nike.cerberus.util;

import com.beust.jcommander.IStringConverter;
import com.nike.cerberus.domain.environment.StackName;

public class StackNameConverter implements IStringConverter<StackName> {

    @Override
    public StackName convert(String value) {
        return StackName.fromName(value);
    }
}