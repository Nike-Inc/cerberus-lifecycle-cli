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

import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.CreateDatabaseCommand;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.input.CerberusStack;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.domain.input.ManagementService;
import com.nike.cerberus.domain.input.VpcAccessWhitelist;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class EnvironmentConfigToArgsMapper {

    public static final String STACK_NAME_KEY = "--stack-name";

    private EnvironmentConfigToArgsMapper() {

    }

    public static String[] getArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        List<String> args = new LinkedList<>();
        String commandName = null;

        // Add all the passed args up to and including the command to run.
        // All other args will be dropped and derived from the yaml
        for (int i = 0; i < passedArgs.length; i++) {
            if (StringUtils.startsWith(passedArgs[i], "-")) {
                args.add(passedArgs[i]);
                if (i < passedArgs.length && ! StringUtils.startsWith(passedArgs[i+1], "-")) {
                    args.add(passedArgs[i+1]);
                    i++;
                }
            } else {
                commandName = passedArgs[i];
                args.add(passedArgs[i]);
                break;
            }
        }

        // now if the command supplied is a command that that is reused for multiple steps, like update-stack, or upload-cert
        // we need to source the args
        if (! StringUtils.isBlank(commandName)) {
            args.addAll(getArgsForCommand(environmentConfig, commandName, passedArgs));
        }

        // convert to string array and return
        return args.toArray(new String[args.size()]);
    }

    public static List<String> getArgsForCommand(EnvironmentConfig environmentConfig, String commandName, String[] passedArgs) {
        switch (commandName) {
            case CreateBaseCommand.COMMAND_NAME:
                return getCreateBaseCommandArgs(environmentConfig);
            case UploadCertFilesCommand.COMMAND_NAME:
                return getUploadCertFilesCommandArgs(environmentConfig, passedArgs);
            case CreateCmsClusterCommand.COMMAND_NAME:
                return getCreateCmsClusterCommandArgs(environmentConfig);
            case WhitelistCidrForVpcAccessCommand.COMMAND_NAME:
                return getWhitelistCidrForVpcAccessCommandArgs(environmentConfig);
            case CreateCmsConfigCommand.COMMAND_NAME:
                return getCreateCmsConfigCommandArgs(environmentConfig);
            case UpdateStackCommand.COMMAND_NAME:
                return getUpdateStackCommandArgs(environmentConfig, passedArgs);
            case UpdateCmsConfigCommand.COMMAND_NAME:
                return getCreateCmsConfigCommandArgs(environmentConfig);
            case CreateVpcCommand.COMMAND_NAME:
                return getCreateVpcCommandArgs(environmentConfig);
            case CreateSecurityGroupsCommand.COMMAND_NAME:
                return getCreateSecurityGroupsCommandArgs(environmentConfig);
            case CreateDatabaseCommand.COMMAND_NAME:
                return getCreateDatabaseCommandArgs(environmentConfig);
            case CreateLoadBalancerCommand.COMMAND_NAME:
                return getCreateLoadBalancerCommandArgs(environmentConfig);
            case CreateRoute53Command.COMMAND_NAME:
                return getCreateRoute53CommandArgs(environmentConfig);
            case CreateWafCommand.COMMAND_NAME:
                return getCreateWafCommandArgs(environmentConfig);
            default:
                List<String> passedArgsList = new LinkedList<>();
                passedArgsList.addAll(Arrays.asList(passedArgs));
                return passedArgsList;
        }
    }

    private static List<String> getCreateCmsConfigCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        ManagementService managementService = environmentConfig.getManagementService();

        args.add(CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG);
        args.add(managementService.getAdminGroup());

        managementService.getProperties().forEach(property -> {
            args.add(CreateCmsConfigCommand.PROPERTY_SHORT_ARG);
            args.add(property);
        });

        return args;
    }

    private static List<String> getWhitelistCidrForVpcAccessCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        VpcAccessWhitelist vpcAccessWhitelist = environmentConfig.getVpcAccessWhitelist();

        vpcAccessWhitelist.getCidrs().forEach(cidr -> {
            args.add(WhitelistCidrForVpcAccessCommand.CIDR_LONG_ARG);
            args.add(cidr);
        });

        vpcAccessWhitelist.getPorts().forEach(port -> {
            args.add(WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG);
            args.add(port);
        });

        return args;
    }

    private static List<String> getCreateCmsClusterCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        ManagementService component = environmentConfig.getManagementService();
        addCommonStackArgs(environmentConfig, args, component);

        return args;
    }

    private static void addCommonStackArgs(EnvironmentConfig environmentConfig, List<String> args, CerberusStack stack) {
        args.add(StackDelegate.AMI_ID_LONG_ARG);
        args.add(stack.getAmiId());
        args.add(StackDelegate.INSTANCE_SIZE_LONG_ARG);
        args.add(stack.getInstanceSize());
        args.add(StackDelegate.KEY_PAIR_NAME_LONG_ARG);
        args.add(stack.getKeyPairName());

        addTagArgs(environmentConfig, args);
    }

    private static void addTagArgs(EnvironmentConfig environmentConfig, List<String> args) {
        args.add(StackDelegate.COST_CENTER_LONG_ARG);
        args.add(environmentConfig.getCostCenter());
        args.add(StackDelegate.OWNER_EMAIL_LONG_ARG);
        args.add(environmentConfig.getOwnerEmail());
        args.add(StackDelegate.OWNER_GROUP_LONG_ARG);
        args.add(environmentConfig.getOwnerGroup());
    }

    private static List<String> getUploadCertFilesCommandArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        String stackName = getStackName(passedArgs);
        List<String> args = new LinkedList<>();

        if (stackName == null) {
            return args;
        }

        args.add(UploadCertFilesCommand.STACK_NAME_LONG_ARG);
        args.add(stackName);

        args.add(UploadCertFilesCommand.CERT_PATH_LONG_ARG);
        switch (stackName) {
            case "vault":
                args.add(environmentConfig.getVault().getCertPath());
                break;
            case "cms":
                args.add(environmentConfig.getManagementService().getCertPath());
                break;
            default:
                args.add("");
        }

        Arrays.stream(passedArgs).forEach(arg -> {
            if (arg.equals("--overwrite")) {
                args.add(UploadCertFilesCommand.OVERWRITE_LONG_ARG);
            }
        });

        return args;
    }

    private static List<String> getCreateBaseCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();

        addTagArgs(config, args);

        args.add(CreateBaseCommand.ADMIN_ROLE_ARN_LONG_ARG);
        args.add(config.getAdminRoleArn());

        return args;
    }

    private static List<String> getUpdateStackCommandArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        String stackName = getStackName(passedArgs);
        List<String> args = new LinkedList<>();

        if (StringUtils.isBlank(stackName)) {
            return args;
        }

        args.add(STACK_NAME_KEY);
        args.add(stackName);

        CerberusStack cerberusStack;
        switch (stackName) {
            case "vault":
                cerberusStack = environmentConfig.getVault();
                break;
            case "cms":
                cerberusStack = environmentConfig.getManagementService();
                break;
            default:
                cerberusStack = null;
        }

        if (cerberusStack != null) {
            addCommonStackArgs(environmentConfig, args, cerberusStack);
        }

        for (int i = 0; i < passedArgs.length; i++) {
            String arg = passedArgs[i];
            if (arg.equals(UpdateStackCommand.OVERWRITE_TEMPLATE_LONG_ARG)) {
                args.add(UpdateStackCommand.OVERWRITE_TEMPLATE_LONG_ARG);
            }
            if (arg.equals(UpdateStackCommand.PARAMETER_SHORT_ARG) && i < passedArgs.length -1) {
                args.add(UpdateStackCommand.PARAMETER_SHORT_ARG);
                args.add(passedArgs[i+1]);
            }
        }

        return args;
    }

    private static List<String> getCreateVpcCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();
        addTagArgs(config, args);
        return args;
    }

    private static List<String> getCreateSecurityGroupsCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();

        if (config.getSecurityGroups() != null &&
                config.getSecurityGroups().getLoadBalancerCidrBlock() != null) {
            args.add(CreateSecurityGroupsCommand.LOAD_BALANCER_CIDR_BLOCK_LONG_ARG);
            args.add(config.getSecurityGroups().getLoadBalancerCidrBlock());
        }

        addTagArgs(config, args);

        return args;
    }

    private static List<String> getCreateDatabaseCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();
        addTagArgs(config, args);
        return args;
    }

    private static List<String> getCreateLoadBalancerCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();
        addTagArgs(config, args);
        return args;
    }

    private static List<String> getCreateRoute53CommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();

        args.add(CreateRoute53Command.HOSTNAME_LONG_ARG);
        args.add(config.getHostname());
        args.add(CreateRoute53Command.HOSTED_ZONE_ID);
        args.add(config.getHostedZoneId());

        return args;
    }

    private static List<String> getCreateWafCommandArgs(EnvironmentConfig config) {
        List<String> args = new LinkedList<>();
        addTagArgs(config, args);
        return args;
    }

    private static String getStackName(String[] passedArgs) {
        for (int i = 0; i < passedArgs.length; i++) {
            if (StringUtils.equals(passedArgs[i], STACK_NAME_KEY)) {
                  return passedArgs[i+1];
            }
        }
        return null;
    }
}
