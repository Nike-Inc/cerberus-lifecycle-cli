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

package com.nike.cerberus.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.validator.EnvironmentNameValidator;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.CerberusCommand.COMMAND_NAME;

/**
 * Global command parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Cerberus Admin and Lifecycle CLI.")
public class CerberusCommand {

    public static final String COMMAND_NAME = "cerberus";

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"--environment", "--env", "-e"},
            description = "Cerberus environment name to execute against.",
            validateWith = EnvironmentNameValidator.class)
    private String environment;

    @Parameter(names = {"--region", "-r"}, description = "The AWS region to execute against.")
    private String region;

    @Parameter(names = {"--debug"}, description = "Enables debug output.")
    private boolean debug;

    @Parameter(names = {"--help", "-h"}, description = "Prints the usage screen for the command.", help = true)
    private boolean help;

    @Parameter(names = {"--version", "-v"}, description = "Prints the version of the CLI.")
    private boolean version;

    @ParametersDelegate
    private ProxyDelegate proxyDelegate = new ProxyDelegate();

    public List<String> getParameters() {
        return parameters;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getRegion() {
        return region;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isVersion() {
        return version;
    }

    public ProxyDelegate getProxyDelegate() {
        return proxyDelegate;
    }
}
