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
import com.google.common.collect.Sets;
import com.nike.cerberus.domain.environment.StackName;

import java.util.Set;

/**
 * Validates that the stack name specified actually requires a certificate to be uploaded.
 */
public class UploadCertFilesStackNameValidator implements IValueValidator<StackName> {

    private final Set<StackName> stackNamesWithCerts = Sets.newHashSet(
            StackName.VAULT, StackName.CMS);

    @Override
    public void validate(final String name, final StackName stackName) throws ParameterException {
        if (!stackNamesWithCerts.contains(stackName)) {
            throw new ParameterException("Stack specified doesn't require a certificate to be uploaded.");
        }
    }
}
