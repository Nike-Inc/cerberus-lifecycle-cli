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
import com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand;
import com.nike.cerberus.command.audit.CreateAuditLoggingStackCommand;
import com.nike.cerberus.command.audit.DisableAuditLoggingCommand;
import com.nike.cerberus.command.audit.EnableAuditLoggingForExistingEnvironmentCommand;
import com.nike.cerberus.command.certificates.RotateAcmeAccountPrivateKeyCommand;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.composite.CreateEnvironmentCommand;
import com.nike.cerberus.command.composite.DeleteEnvironmentCommand;
import com.nike.cerberus.command.certificates.GenerateAndRotateCertificatesCommand;
import com.nike.cerberus.command.composite.PrintAllStackInformationCommand;
import com.nike.cerberus.command.certificates.RotateCertificatesCommand;
import com.nike.cerberus.command.core.InitializeEnvironmentCommand;
import com.nike.cerberus.command.rds.CleanUpRdsSnapshotsCommand;
import com.nike.cerberus.command.rds.CopyRdsSnapshotsCommand;
import com.nike.cerberus.command.rds.CreateDatabaseCommand;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.command.certificates.DeleteOldestCertificatesCommand;
import com.nike.cerberus.command.core.DeleteStackCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.command.core.RestoreCerberusBackupCommand;
import com.nike.cerberus.command.core.RebootCmsCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.certificates.UploadCertificateFilesCommand;
import com.nike.cerberus.command.core.ViewConfigCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.command.core.UpdateStackTagsCommand;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.logging.LoggingConfigurer;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.module.PropsModule;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.util.LocalEnvironmentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            final Logger log = LoggerFactory.getLogger(getClass());

            String commandName = commander.getParsedCommand();
            Command command = commandMap.get(commandName);

            if (cerberusCommand.isVersion()) {
                printCliVersion();
            } else if (cerberusCommand.isHelp() || commandName == null) {
                cerberusHelp.print();
            } else {
                Injector injector = Guice.createInjector(new CerberusModule(cerberusCommand), new PropsModule());

                // fail early if there is any problem in local environment
                LocalEnvironmentValidator validator = injector.getInstance(LocalEnvironmentValidator.class);
                validator.validate();

                Operation operation = injector.getInstance(command.getOperationClass());

                if (operation.isRunnable(command)) {
                    log.info("Running command: {}", commandName);
                    operation.run(command);
                    log.info("Finished command: {}", commandName);
                } else {
                    throw new RuntimeException("Command not runnable");
                }
            }
        } catch (Throwable e) {
            if (cerberusCommand.isHelp()) {
                cerberusHelp.print();
            } else {
                System.err.println(Chalk.on("ERROR: " + e.getMessage()).red().bold().toString());
                e.printStackTrace();
                cerberusHelp.print();
                System.exit(1);
            }
        }

        // Shutdown the thread pool executors
        System.exit(0);
    }

    /**
     * If --file, -f was passed in we will map the dsl params to args.
     * <p>
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
        registerCommand(new InitializeEnvironmentCommand());
        registerCommand(new UploadCertificateFilesCommand());
        registerCommand(new CreateCmsConfigCommand());
        registerCommand(new CreateCmsClusterCommand());
        registerCommand(new UpdateStackCommand());
        registerCommand(new PrintStackInfoCommand());
        registerCommand(new PrintAllStackInformationCommand());
        registerCommand(new WhitelistCidrForVpcAccessCommand());
        registerCommand(new RestoreCerberusBackupCommand());
        registerCommand(new ViewConfigCommand());
        registerCommand(new UpdateCmsConfigCommand());
        registerCommand(new RebootCmsCommand());
        registerCommand(new GenerateCertificateFilesCommand());
        registerCommand(new CreateVpcCommand());
        registerCommand(new CreateWafCommand());
        registerCommand(new CreateDatabaseCommand());
        registerCommand(new CreateRoute53Command());
        registerCommand(new CreateSecurityGroupsCommand());
        registerCommand(new CreateLoadBalancerCommand());
        registerCommand(new CreateEdgeDomainRecordCommand());
        registerCommand(new CreateEnvironmentCommand());
        registerCommand(new DeleteStackCommand());
        registerCommand(new DeleteEnvironmentCommand());
        registerCommand(new RotateCertificatesCommand());
        registerCommand(new DeleteOldestCertificatesCommand());
        registerCommand(new CopyRdsSnapshotsCommand());
        registerCommand(new GenerateAndRotateCertificatesCommand());
        registerCommand(new RotateAcmeAccountPrivateKeyCommand());
        registerCommand(new CleanUpRdsSnapshotsCommand());
        registerCommand(new CreateAuditLoggingStackCommand());
        registerCommand(new CreateAuditAthenaDbAndTableCommand());
        registerCommand(new DisableAuditLoggingCommand());
        registerCommand(new EnableAuditLoggingForExistingEnvironmentCommand());
        registerCommand(new UpdateStackTagsCommand());
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
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        CerberusRunner runner = new CerberusRunner();
        runner.run(args);
    }
}