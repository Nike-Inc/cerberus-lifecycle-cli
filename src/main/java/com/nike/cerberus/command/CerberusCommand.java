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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nike.cerberus.command.validator.EnvironmentNameValidator;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.CerberusCommand.COMMAND_NAME;

/**
 * Global command parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Cerberus Admin and Lifecycle CLI.")
public class CerberusCommand {

    public static final String COMMAND_NAME = "cerberus";

    private EnvironmentConfig environmentConfig;

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"--file", "-f"}, description = "The environment yaml file, allows users to define their environments in a yaml file rather than passing args for each command. This file is required when using composite commands. If file is supplied all args passed for individual commands are ignored and sourced from the yaml, except on commands that require special identifiers like update-stack and --stack-name, which would be used to point to what stack you want to update in the yaml")
    private String file;

    @Parameter(names = {"--environment", "--env", "-e"},
            description = "Cerberus environment name to execute against. This is required to be set via 'CERBERUS_CLI_ENV' env var or supplied via command arg",
            validateWith = EnvironmentNameValidator.class)
    private String environment;

    @Parameter(names = {"--region", "-r"}, description = "The AWS region to execute against. This is required to be set via 'CERBERUS_CLI_REGION' env var or supplied via command arg")
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

    public EnvironmentConfig getEnvironmentConfig() {
        if (environmentConfig != null) {
            return environmentConfig;
        }

        if (file == null) {
            return null;
        }

        File environmentConfigFile = new File(file);
        if (! environmentConfigFile.exists() || environmentConfigFile.isDirectory()) {
            throw new IllegalArgumentException(String.format("The file: %s does not exist or is a directory", file));
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        try {
            environmentConfig = mapper.readValue(environmentConfigFile, EnvironmentConfig.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize environment yaml", e);
        }

        return environmentConfig;
    }

    /**
     * Will lookup env value in the following order
     * 1. Passed in arg ex: -e, --env, --environment
     * 2. If an env yaml was supplied use the env name defined there
     * 3. If 1 and 2 fail look for value in CERBERUS_CLI_ENV env var
     */
    public String getEnvironment() {
        String commandLinePassedEnvironment = environment;
        String environmentConfigFileEnvironment = getEnvironmentConfig() == null ? null : getEnvironmentConfig().getEnvironmentName();
        String EnvironmentalVarEnvironment = System.getenv("CERBERUS_CLI_ENV");

        String calculatedEnv = StringUtils.isNotBlank(commandLinePassedEnvironment) ? commandLinePassedEnvironment :
                StringUtils.isNotBlank(environmentConfigFileEnvironment) ? environmentConfigFileEnvironment :
                        EnvironmentalVarEnvironment;

        if (StringUtils.isBlank(calculatedEnv)) {
            throw new IllegalArgumentException("Failed to determine environment, checked 'CERBERUS_CLI_ENV' env var and -e, --env, --environment command options, options must go before the command");
        }

        return calculatedEnv;
    }

    /**
     * Will lookup region value in the following order
     * 1. Passed in arg ex: -r, --region
     * 2. If an env yaml was supplied use the region defined there
     * 3. If 1 and 2 fail look for value in CERBERUS_CLI_REGION env var
     */
    public String getRegion() {
        String commandLinePassedRegion = region;
        String environmentConfigFileRegion = getEnvironmentConfig() == null ? null : getEnvironmentConfig().getRegion();
        String EnvironmentalVarRegion = System.getenv("CERBERUS_CLI_REGION");

        String calculatedRegion = StringUtils.isNotBlank(commandLinePassedRegion) ? commandLinePassedRegion :
                StringUtils.isNotBlank(environmentConfigFileRegion) ? environmentConfigFileRegion :
                        EnvironmentalVarRegion;

        if (StringUtils.isBlank(calculatedRegion)) {
            throw new IllegalArgumentException("Failed to determine environment, checked 'CERBERUS_CLI_REGION' env var and -r, --region command options, options must go before the command");
        }

        return calculatedRegion;
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
