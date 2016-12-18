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

package com.nike.cerberus.cli;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import com.github.tomaslanger.chalk.Chalk;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.CerberusCommand;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.consul.CreateConsulClusterCommand;
import com.nike.cerberus.command.consul.CreateConsulConfigCommand;
import com.nike.cerberus.command.consul.CreateVaultAclCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.EnableConfigReplicationCommand;
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.command.dashboard.PublishDashboardCommand;
import com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand;
import com.nike.cerberus.command.gateway.CreateCloudFrontSecurityGroupUpdaterLambdaCommand;
import com.nike.cerberus.command.gateway.CreateGatewayClusterCommand;
import com.nike.cerberus.command.gateway.CreateGatewayConfigCommand;
import com.nike.cerberus.command.gateway.PublishLambdaCommand;
import com.nike.cerberus.command.vault.CreateCmsVaultTokenCommand;
import com.nike.cerberus.command.vault.CreateVaultClusterCommand;
import com.nike.cerberus.command.vault.CreateVaultConfigCommand;
import com.nike.cerberus.command.vault.DisableAuditBackendCommand;
import com.nike.cerberus.command.vault.EnableAuditBackendCommand;
import com.nike.cerberus.command.vault.InitVaultClusterCommand;
import com.nike.cerberus.command.vault.LoadDefaultVaultPoliciesCommand;
import com.nike.cerberus.command.vault.UnsealVaultClusterCommand;
import com.nike.cerberus.command.vault.VaultHealthCheckCommand;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.logging.LoggingConfigurer;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.module.PropsModule;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.util.LocalEnvironmentValidator;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * CLI entry point.
 */
public class CerberusRunner {

    private final Map<String, Command> commandMap;
    private CerberusCommand cerberusCommand;
    private final JCommander commander;

    private CerberusRunner() {
        commandMap = Maps.newHashMap();
        cerberusCommand = new CerberusCommand();
        commander = new JCommander(cerberusCommand);
        commander.setProgramName("cerberus");
        commander.setAcceptUnknownOptions(true);
    }

    /**
     * Actual application runner.  Determines which command is specified and executes the associated operation.
     *
     * @param args Command line arguments
     */
    @SuppressWarnings("unchecked")
    public void run(String[] args) {
        registerAllCommands();

        try {
            args = getEnvironmentalConfigArgs(args);

            commander.parse(args);

            configureLogging(cerberusCommand.isDebug());
            final String commandName = commander.getParsedCommand();
            Command command = commandMap.get(commandName);

            final Injector propsInjector = Guice.createInjector(new PropsModule());

            if(cerberusCommand.isVersion()) {
                String version = propsInjector.getInstance(Key.get(String.class, Names.named(ConfigConstants.VERSION_PROPERTY)));
                String versionMessage = Chalk.on(String.format("Cerberus Lifecycle CLI version: %s", version)).green().bold().toString();
                System.out.println(versionMessage);
            } else if (cerberusCommand.isHelp() || commandName == null) {
                if (StringUtils.isNotBlank(commandName)) {
                    commander.usage(commandName);
                } else {
                    printCustomUsage();
                }
            } else {
                final Injector injector = Guice.createInjector(new CerberusModule(cerberusCommand.getProxyDelegate(),
                        cerberusCommand.getEnvironment(), cerberusCommand.getRegion()), new PropsModule());

                // fail early if there is any problem in local environment
                LocalEnvironmentValidator validator = injector.getInstance(LocalEnvironmentValidator.class);
                validator.validate();

                final Operation operation = injector.getInstance(command.getOperationClass());

                if (operation.isRunnable(command)) {
                    operation.run(command);
                }
            }
        } catch (Throwable e) {
            if (! cerberusCommand.isHelp()) {
                System.err.println(Chalk.on("ERROR: " + e.getMessage()).red().bold().toString());
            }

            String commandName = commander.getParsedCommand();
            if (StringUtils.isNotBlank(commandName)) {
                commander.usage(commandName);
            } else {
                printCustomUsage();
            }
        }
    }

    /**
     * If --file, -f was passed in we will map the dsl params to args.
     *
     * Due to the way jCommander works and validates args, we will create a new local command and parse the args
     * without validation and get the env config and return the new args back to the main commander to parse.
     *
     * @param args The args passed into CLI from the user
     * @return merged arg array containing file params merged with user params
     */
    @VisibleForTesting
    private String[] getEnvironmentalConfigArgs(String[] args) {
        CerberusCommand cerberusCommand = new CerberusCommand();
        JCommander commander = new JCommander(cerberusCommand);
        commander.setProgramName("cerberus");
        commander.setAcceptUnknownOptions(true);
        commander.parseWithoutValidation(args);

        EnvironmentConfig environmentConfig = cerberusCommand.getEnvironmentConfig();

        if (environmentConfig != null) {
            return EnvironmentConfigToArgsMapper.getArgs(environmentConfig, args);
        }
        return args;
    }

    private void printCommands() {
        System.out.println("Commands, use cerberus [-h, --help] [command name] for more info:");
        commander.getCommands().keySet().forEach(command -> {
            String msg = String.format("    %s, %s",
                    Chalk.on(command).green().bold().toString(),
                    commander.getCommandDescription(command));
            System.out.println(msg);
        });
    }

    private void printCustomUsage() {
        StringBuilder sb = new StringBuilder("Usage: cerberus [options] [command] [command options]\n");

        String indent = "";
        //indenting
        int descriptionIndent = 6;
        int indentCount = indent.length() + descriptionIndent;

        int longestName = 0;
        List<ParameterDescription> sorted = Lists.newArrayList();
        for (ParameterDescription pd : commander.getParameters()) {
            if (! pd.getParameter().hidden()) {
                sorted.add(pd);
                // + to have an extra space between the name and the description
                int length = pd.getNames().length() + 2;
                if (length > longestName) {
                    longestName = length;
                }
            }
        }

        sb.append(indent).append("  Options:\n");

        sorted.stream()
                .sorted((p0, p1) -> p0.getLongestName().compareTo(p1.getLongestName()))
                .forEach(pd -> {
                    WrappedParameter parameter = pd.getParameter();
                    sb.append(indent).append("  "
                            + (parameter.required() ? "* " : "  ")
                            + Chalk.on(pd.getNames()).green().bold().toString()
                            + "\n");
                    wrapDescription(sb, indentCount, s(indentCount) + pd.getDescription());
                    Object def = pd.getDefault();
                    if (def != null) {
                        String displayedDef = StringUtils.isBlank(def.toString())
                                ? "<empty string>"
                                : def.toString();
                        sb.append("\n" + s(indentCount)).append("Default: " + Chalk.on(displayedDef).yellow().bold().toString());
                    }
                    Class<?> type =  pd.getParameterized().getType();
                    if(type.isEnum()){
                        String values = EnumSet.allOf((Class<? extends Enum>) type).toString();
                        sb.append("\n" + s(indentCount)).append("Possible Values: " + Chalk.on(values).yellow().bold().toString());
                    }
                    sb.append("\n");
                });

        System.out.println(sb.toString());
        System.out.print("  ");
        printCommands();
    }

    private String s(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(" ");
        }

        return result.toString();
    }

    private void wrapDescription(StringBuilder out, int indent, String description) {
        int max = 79;
        String[] words = description.split(" ");
        int current = 0;
        int i = 0;
        while (i < words.length) {
            String word = words[i];
            if (word.length() > max || current + 1 + word.length() <= max) {
                out.append(word).append(" ");
                current += word.length() + 1;
            } else {
                out.append("\n").append(s(indent)).append(word).append(" ");
                current = indent + 1 + word.length();
            }
            i++;
        }
    }

    /**
     * Convenience method for registering all top level commands.
     */
    private void registerAllCommands() {
        registerCommand(new CreateBaseCommand());
        registerCommand(new UploadCertFilesCommand());
        registerCommand(new CreateConsulConfigCommand());
        registerCommand(new CreateVaultAclCommand());
        registerCommand(new CreateConsulClusterCommand());
        registerCommand(new CreateVaultConfigCommand());
        registerCommand(new CreateVaultClusterCommand());
        registerCommand(new EnableConfigReplicationCommand());
        registerCommand(new PublishDashboardCommand());
        registerCommand(new InitVaultClusterCommand());
        registerCommand(new UnsealVaultClusterCommand());
        registerCommand(new EnableAuditBackendCommand());
        registerCommand(new DisableAuditBackendCommand());
        registerCommand(new LoadDefaultVaultPoliciesCommand());
        registerCommand(new CreateCmsVaultTokenCommand());
        registerCommand(new CreateCmsConfigCommand());
        registerCommand(new CreateCmsClusterCommand());
        registerCommand(new CreateGatewayConfigCommand());
        registerCommand(new CreateGatewayClusterCommand());
        registerCommand(new UpdateStackCommand());
        registerCommand(new PrintStackInfoCommand());
        registerCommand(new VaultHealthCheckCommand());
        registerCommand(new PublishLambdaCommand());
        registerCommand(new CreateCloudFrontLogProcessingLambdaConfigCommand());
        registerCommand(new CreateCloudFrontSecurityGroupUpdaterLambdaCommand());
        registerCommand(new WhitelistCidrForVpcAccessCommand());
    }

    /**
     * Registers a command with the command map and commander.
     *
     * @param command Command to be registered
     */
    private void registerCommand(final Command command) {
        commandMap.put(command.getCommandName(), command);
        commander.addCommand(command.getCommandName(), command);
    }

    /**
     * Configures the logging backend.
     *
     * @param isDebug Whether debug logging is enabled
     */
    private void configureLogging(boolean isDebug) {
        Level logLevel = Level.INFO;
        if (isDebug) {
            logLevel = Level.DEBUG;
        }

        LoggingConfigurer.configure(logLevel);
    }

    /**
     * Program entry point.  Instantiates and calls run.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        CerberusRunner runner = new CerberusRunner();
        runner.run(args);
    }
}