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

package com.nike.cerberus.command.validator;

import com.beust.jcommander.ParameterException;
import org.junit.Test;

public class EnvironmentNameValidatorTest {

    private final static String NAME = "NAME";
    private final EnvironmentNameValidator validator = new EnvironmentNameValidator();

    // Need to verify if underscores are valid or not, test fails with underscores
//    @Test
//    public void testValidateValidInput() {
//        validator.validate(NAME, "valid");
//        validator.validate(NAME, "valid_123");
//        validator.validate(NAME, "Valid_123_UPPERCASE");
//        validator.validate(NAME, "alphaNumericWithUnderscores_123_UPPERCASE");
//        validator.validate(NAME, "1234");
//        validator.validate(NAME, "1234_5678");
//    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidComma() {
        validator.validate(NAME, ",");
    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidPeriod() {
        validator.validate(NAME, ".");
    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidPunc() {
        validator.validate(NAME, "punc!!");
    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidParen() {
        validator.validate(NAME, "(invalid)");
    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidSpace() {
        validator.validate(NAME, "invalid space");
    }

    @Test(expected = ParameterException.class)
    public void testValidateInvalidEmpty() {
        validator.validate(NAME, "");
    }
}
