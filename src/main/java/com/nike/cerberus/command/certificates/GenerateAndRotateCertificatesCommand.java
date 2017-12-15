/*
 * Copyright (c) 2017 Nike, Inc.
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

package com.nike.cerberus.command.certificates;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.certificates.GenerateAndRotateCertificatesOperation;

import static com.nike.cerberus.command.certificates.GenerateAndRotateCertificatesCommand.COMMAND_NAME;

@Parameters(
        commandNames = {
                COMMAND_NAME
        },
        commandDescription = "Generates certificates with an ACME certificate provider and performs the necessary " +
                "steps to rotate the certificates used by the ALB and CMS"
)
public class GenerateAndRotateCertificatesCommand implements Command {

    public static final String COMMAND_NAME = "generate-and-rotate-certificates";

    @ParametersDelegate
    private GenerateCertificateFilesCommandParametersDelegate generateCertificateFilesCommandParametersDelegate =
            new GenerateCertificateFilesCommandParametersDelegate();

    public GenerateCertificateFilesCommandParametersDelegate getGenerateCertificateFilesCommandParametersDelegate() {
        return generateCertificateFilesCommandParametersDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return GenerateAndRotateCertificatesOperation.class;
    }
}
