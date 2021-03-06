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

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.nio.file.Path;

/**
 * Validates that the specified directory contains all the correct files for cert upload.
 */
public class UploadCertFilesPathValidator implements IValueValidator<Path> {

    @Override
    public void validate(final String name, final Path value) throws ParameterException {

        if (value == null) {
            throw new ParameterException("Value must be specified.");
        }

        final File certDirectory = value.toFile();

        if (!certDirectory.canRead()) {
            throw new ParameterException(String.format("Specified path: %s is not readable.", certDirectory.getAbsolutePath()));
        }

        if (!certDirectory.isDirectory()) {
            throw new ParameterException(String.format("Specified path: %s is not a directory.", certDirectory.getAbsolutePath()));
        }
    }
}
