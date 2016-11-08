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

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Sets;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;

import static com.nike.cerberus.operation.core.UploadCertFilesOperation.EXPECTED_FILE_NAMES;

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
        final Set<String> filenames = Sets.newHashSet();

        if (!certDirectory.canRead()) {
            throw new ParameterException("Specified path is not readable.");
        }

        if (!certDirectory.isDirectory()) {
            throw new ParameterException("Specified path is not a directory.");
        }


        final FilenameFilter filter = new RegexFileFilter("^.*\\.pem$");
        final File[] files = certDirectory.listFiles(filter);
        Arrays.stream(files).forEach(file -> filenames.add(file.getName()));

        if (!filenames.containsAll(EXPECTED_FILE_NAMES)) {
            final StringJoiner sj = new StringJoiner(", ", "[", "]");
            EXPECTED_FILE_NAMES.stream().forEach(sj::add);
            throw new ParameterException("Not all expected files are present! Expected: " + sj.toString());
        }
    }
}
