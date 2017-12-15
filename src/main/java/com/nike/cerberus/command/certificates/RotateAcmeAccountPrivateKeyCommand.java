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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.certificates.RotateAcmeAccountPrivateKeyOperation;

import static com.nike.cerberus.command.certificates.RotateAcmeAccountPrivateKeyCommand.COMMAND_NAME;

@Parameters(
        commandNames = {
                COMMAND_NAME
        },
        commandDescription = "Rotates the ACME Account private key."
)
public class RotateAcmeAccountPrivateKeyCommand implements Command {

    public static final String COMMAND_NAME = "rotate-acme-account-key";

    public static final String ACME_URL_LONG_ARG = "--acme-url";

    @Parameter(
            names = ACME_URL_LONG_ARG,
            description = "The ACME API URL to use.",
            required = true
    )
    private String acmeUrl;

    public String getAcmeUrl() {
        return acmeUrl;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return RotateAcmeAccountPrivateKeyOperation.class;
    }
}
