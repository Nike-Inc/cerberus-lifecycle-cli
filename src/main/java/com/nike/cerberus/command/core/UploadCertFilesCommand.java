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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.validator.UploadCertFilesPathValidator;
import com.nike.cerberus.command.validator.UploadCertFilesStackNameValidator;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.UploadCertFilesOperation;

import java.nio.file.Path;

import static com.nike.cerberus.command.core.UploadCertFilesCommand.COMMAND_NAME;

/**
 * Command for uploading a certificate for a specific component.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Upload certificate files for a specific component.")
public class UploadCertFilesCommand implements Command {

    public static final String COMMAND_NAME = "upload-cert";

    @Parameter(
            names = {"--stack-name"},
            required = true,
            description = "Stack name the certificate is for.",
            validateValueWith = UploadCertFilesStackNameValidator.class)
    private StackName stackName;

    @Parameter(
            names = {"--cert-path"},
            required = true,
            description = "Path to the directory that contains the certificate files.",
            validateValueWith = UploadCertFilesPathValidator.class)
    private Path certPath;

    @Parameter(
            names = {"--overwrite"},
            description = "If certificate is already present for the environment and component, overwrite it."
    )
    private boolean overwrite;

    public StackName getStackName() {
        return stackName;
    }

    public Path getCertPath() {
        return certPath;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UploadCertFilesOperation.class;
    }
}
