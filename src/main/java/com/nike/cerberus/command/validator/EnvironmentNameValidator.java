/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.command.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.util.regex.Pattern;

/**
 * Validates that the environment name is made of the correct characters.
 */
public class EnvironmentNameValidator implements IParameterValidator {

    public static final Pattern VALID_NAME_PATTERN = Pattern.compile("[A-Za-z\\d-]+");
    public static final String ALLOWED_DESCRIPTION = "Environment name may only contain alpha-numeric characters and hyphens.";

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!VALID_NAME_PATTERN.matcher(value).matches()) {
            throw new ParameterException(ALLOWED_DESCRIPTION);
        }
    }
}
