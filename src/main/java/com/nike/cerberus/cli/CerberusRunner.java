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
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
import com.nike.cerberus.command.vault.StoreOneLoginApiKeyCommand;
import com.nike.cerberus.command.vault.UnsealVaultClusterCommand;
import com.nike.cerberus.command.vault.VaultHealthCheckCommand;
import com.nike.cerberus.logging.LoggingConfigurer;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.operation.Operation;

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
            commander.parse(args);
            configureLogging(cerberusCommand.isDebug());
            final String commandName = commander.getParsedCommand();
            final Command command = commandMap.get(commandName);

            if (command == null) {
                System.err.println("Unknown command: " + commandName);
                commander.usage();
            } else {
                if (cerberusCommand.isHelp()) {
                    commander.usage(commandName);
                } else {
                    final Injector injector = Guice.createInjector(new CerberusModule(cerberusCommand.getProxyDelegate(),
                            cerberusCommand.getEnvironment(), cerberusCommand.getRegion()));

                    final Operation operation = injector.getInstance(command.getOperationClass());

                    if (operation.isRunnable(command)) {
                        operation.run(command);
                    }
                }
            }
        } catch (ParameterException pe) {
            System.err.println(pe.getMessage());
            commander.usage();
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
        registerCommand(new StoreOneLoginApiKeyCommand());
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