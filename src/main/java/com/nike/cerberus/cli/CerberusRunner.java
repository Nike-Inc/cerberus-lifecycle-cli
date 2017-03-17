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
import com.github.tomaslanger.chalk.Chalk;
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
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.command.core.RestoreCompleteCerberusDataFromS3BackupCommand;
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

import java.util.Map;

/**
 * CLI entry point.
 */
public class CerberusRunner {

    private final Map<String, Command> commandMap;
    private CerberusCommand cerberusCommand;
    private final JCommander commander;
    private final CerberusHelp cerberusHelp;

    private CerberusRunner() {
        commandMap = Maps.newHashMap();
        cerberusCommand = new CerberusCommand();
        commander = new JCommander(cerberusCommand);
        commander.setProgramName("cerberus");
        commander.setAcceptUnknownOptions(true);
        cerberusHelp = new CerberusHelp(commander);
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
            String commandName = commander.getParsedCommand();
            Command command = commandMap.get(commandName);

            if(cerberusCommand.isVersion()) {
                printCliVersion();
            } else if (cerberusCommand.isHelp() || commandName == null) {
                cerberusHelp.print();
            } else {
                Injector injector = Guice.createInjector(new CerberusModule(cerberusCommand.getProxyDelegate(),
                        cerberusCommand.getEnvironment(), cerberusCommand.getRegion()), new PropsModule());

                // fail early if there is any problem in local environment
                LocalEnvironmentValidator validator = injector.getInstance(LocalEnvironmentValidator.class);
                validator.validate();

                Operation operation = injector.getInstance(command.getOperationClass());

                if (operation.isRunnable(command)) {
                    operation.run(command);
                }
            }
        } catch (Throwable e) {
            if (cerberusCommand.isHelp()) {
                cerberusHelp.print();
            }
            else {
                System.err.println(Chalk.on("ERROR: " + e.getMessage()).red().bold().toString());
                e.printStackTrace();
                cerberusHelp.print();
                System.exit(1);
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

    private void printCliVersion() {
        Injector propsInjector = Guice.createInjector(new PropsModule());
        String version = propsInjector.getInstance(Key.get(String.class, Names.named(ConfigConstants.VERSION_PROPERTY)));
        String versionMessage = Chalk.on(String.format("Cerberus Lifecycle CLI version: %s", version)).green().bold().toString();
        System.out.println(versionMessage);
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
        registerCommand(new RestoreCompleteCerberusDataFromS3BackupCommand());
    }

    /**
     * Registers a command with the command map and commander.
     *
     * @param command Command to be registered
     */
    private void registerCommand(Command command) {
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