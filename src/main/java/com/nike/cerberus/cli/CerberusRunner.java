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
import com.beust.jcommander.Parameters;
import com.github.tomaslanger.chalk.Chalk;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.BaseCerberusCommand;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.ProxyDelegate;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsCmkCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.core.CreateCerberusBackupCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.RollingRebootWithHealthCheckCommand;
import com.nike.cerberus.command.core.ViewConfigCommand;
import com.nike.cerberus.command.core.PrintStackInfoCommand;
import com.nike.cerberus.command.core.RestoreCerberusBackupCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.command.core.SetBackupAdminPrincipalsCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.logging.LoggingConfigurer;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.module.PropsModule;
import com.nike.cerberus.util.LocalEnvironmentValidator;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * CLI entry point.
 */
public class CerberusRunner {

    private BaseCerberusCommand baseCerberusCommand;

    private final JCommander commander;

    private final CerberusHelp cerberusHelp;

    private final Map<String, Command> commandMap = new HashMap<>();

    public CerberusRunner() {
        baseCerberusCommand = new BaseCerberusCommand();
        commander = new JCommander(baseCerberusCommand);
        commander.setProgramName("cerberus");
        commander.setColumnSize(120);
        commander.setAcceptUnknownOptions(true);
        cerberusHelp = new CerberusHelp(commander);
    }

    @Inject
    public CerberusRunner(JCommander commander,
                          CerberusHelp cerberusHelp,
                          BaseCerberusCommand baseCerberusCommand,
                          CreateVpcCommand createVpcCommand,
                          UploadCertFilesCommand uploadCertFilesCommand,
                          CreateCmsConfigCommand createCmsConfigCommand,
                          CreateCmsClusterCommand createCmsClusterCommand,
                          CreateCmsCmkCommand createCmsCmkCommand,
                          UpdateStackCommand updateStackCommand,
                          PrintStackInfoCommand printStackInfoCommand,
                          WhitelistCidrForVpcAccessCommand whitelistCidrForVpcAccessCommand,
                          RestoreCerberusBackupCommand restoreCerberusBackupCommand,
                          ViewConfigCommand viewConfigCommand,
                          UpdateCmsConfigCommand updateCmsConfigCommand,
                          RollingRebootWithHealthCheckCommand rollingRebootWithHealthCheckCommand,
                          CreateCerberusBackupCommand createCerberusBackupCommand,
                          SetBackupAdminPrincipalsCommand setBackupAdminPrincipalsCommand) {

        this.baseCerberusCommand = baseCerberusCommand;
        this.commander = commander;
        this.cerberusHelp = cerberusHelp;

        registerCommand(createVpcCommand);
        registerCommand(uploadCertFilesCommand);
        registerCommand(createCmsConfigCommand);
        registerCommand(createCmsClusterCommand);
        registerCommand(createCmsCmkCommand);
        registerCommand(updateStackCommand);
        registerCommand(printStackInfoCommand);
        registerCommand(whitelistCidrForVpcAccessCommand);
        registerCommand(restoreCerberusBackupCommand);
        registerCommand(viewConfigCommand);
        registerCommand(updateCmsConfigCommand);
        registerCommand(rollingRebootWithHealthCheckCommand);
        registerCommand(createCerberusBackupCommand);
        registerCommand(setBackupAdminPrincipalsCommand);
    }

    /**
     * Commands are Runnable's with JCommander annotations
     */
    public void registerCommand(Command command) {
        Parameters p = command.getClass().getAnnotation(Parameters.class);
        if (p != null && p.commandNames().length > 0) {
            for (String commandName : p.commandNames()) {
                commander.addCommand(commandName, command);
                commandMap.put(commandName, command);
            }
        } else {
            throw new RuntimeException("Trying to add command " + command.getClass().getName()
                    + " without specifying its names in @Parameters");
        }
    }

    /**
     * Actual application runner.  Determines which command is specified and executes the associated operation.
     *
     * @param args Command line arguments
     */
    @SuppressWarnings("unchecked")
    public void run(String[] args) {
        try {
            args = getEnvironmentalConfigArgs(args);

            commander.parse(args);

            configureLogging(baseCerberusCommand.isDebug());
            String commandName = commander.getParsedCommand();
            Command command = commandMap.get(commandName);

            if(baseCerberusCommand.isVersion()) {
                printCliVersion();
            } else if (baseCerberusCommand.isHelp() || commandName == null) {
                cerberusHelp.print();
            } else {
                Injector injector = Guice.createInjector(new CerberusModule(baseCerberusCommand.getProxyDelegate(),
                        baseCerberusCommand.getEnvironment(), baseCerberusCommand.getRegion()), new PropsModule());

                // fail early if there is any problem in local environment
                LocalEnvironmentValidator validator = injector.getInstance(LocalEnvironmentValidator.class);
                validator.validate();

                if (command.isRunnable()) {
                    command.execute();
                } else {
                    throw new RuntimeException("Command not runnable");
                }
            }
        } catch (Throwable e) {
            if (baseCerberusCommand.isHelp()) {
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
        BaseCerberusCommand baseCerberusCommand = new BaseCerberusCommand();
        JCommander commander = new JCommander(baseCerberusCommand);
        commander.setProgramName("cerberus");
        commander.setAcceptUnknownOptions(true);
        commander.parseWithoutValidation(args);

        EnvironmentConfig environmentConfig = baseCerberusCommand.getEnvironmentConfig();

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