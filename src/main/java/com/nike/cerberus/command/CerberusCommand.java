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
            required = true,
            validateWith = EnvironmentNameValidator.class)
    private String environment;

    @Parameter(names = {"--region", "-r"}, description = "The AWS region to execute against.", required = true)
    private String region;

    @Parameter(names = {"--debug"}, description = "Enables debug output.")
    private boolean debug;

    @Parameter(names = {"--help", "-h"}, description = "Prints the usage screen for the command.", help = true)
    private boolean help;

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

    public ProxyDelegate getProxyDelegate() {
        return proxyDelegate;
    }
}
