package com.nike.cerberus.command.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.util.regex.Pattern;

/**
 * Validates that the environment name is made of the correct characters.
 */
public class EnvironmentNameValidator implements IParameterValidator {

    private final Pattern pattern = Pattern.compile("\\w+");

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!pattern.matcher(value).matches()) {
            throw new ParameterException("Environment name may only contain alpha-numeric characters and underscores.");
        }
    }
}
