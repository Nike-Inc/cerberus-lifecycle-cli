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
import com.nike.cerberus.command.consul.CreateConsulClusterCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.command.dashboard.PublishDashboardCommand;
import com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand;
import com.nike.cerberus.command.gateway.CreateGatewayClusterCommand;
import com.nike.cerberus.command.gateway.CreateGatewayConfigCommand;
import com.nike.cerberus.command.gateway.PublishLambdaCommand;
import com.nike.cerberus.command.vault.CreateVaultClusterCommand;
import com.nike.cerberus.domain.input.CerberusStack;
import com.nike.cerberus.domain.input.Consul;
import com.nike.cerberus.domain.input.Dashboard;
import com.nike.cerberus.domain.input.EdgeSecurity;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.domain.input.Gateway;
import com.nike.cerberus.domain.input.ManagementService;
import com.nike.cerberus.domain.input.Vault;
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

    private static List<String> getArgsForCommand(EnvironmentConfig environmentConfig, String commandName, String[] passedArgs) {
        switch (commandName) {
            case CreateBaseCommand.COMMAND_NAME:
                return getCreateBaseCommandArgs(environmentConfig);
            case UploadCertFilesCommand.COMMAND_NAME:
                return getUploadCertFilesCommandArgs(environmentConfig, passedArgs);
            case CreateConsulClusterCommand.COMMAND_NAME:
                return getCreateConsulClusterCommandArgs(environmentConfig);
            case CreateVaultClusterCommand.COMMAND_NAME:
                return getCreateVaultClusterCommandArgs(environmentConfig);
            case CreateCmsClusterCommand.COMMAND_NAME:
                return getCreateCmsClusterCommandArgs(environmentConfig);
            case CreateGatewayClusterCommand.COMMAND_NAME:
                return getCreateGatewayClusterCommandArgs(environmentConfig);
            case PublishDashboardCommand.COMMAND_NAME:
                return getPublishDashboardCommandArgs(environmentConfig);
            case WhitelistCidrForVpcAccessCommand.COMMAND_NAME:
                return getWhitelistCidrForVpcAccessCommandArgs(environmentConfig);
            case CreateCmsConfigCommand.COMMAND_NAME:
                return getCreateCmsConfigCommandArgs(environmentConfig);
            case CreateGatewayConfigCommand.COMMAND_NAME:
                return getCreateGatewayClusterConfigCommandArgs(environmentConfig);
            case UpdateStackCommand.COMMAND_NAME:
                return getUpdateStackCommandArgs(environmentConfig, passedArgs);
            case PublishLambdaCommand.COMMAND_NAME:
                return getPublishLambdaCommandArgs(environmentConfig, passedArgs);
            case CreateCloudFrontLogProcessingLambdaConfigCommand.COMMAND_NAME:
                return getCreateCloudFrontLogProcessingLambdaConfigCommandArgs(environmentConfig);
            default:
                return new LinkedList<>();
        }
    }

    private static List<String> getCreateCloudFrontLogProcessingLambdaConfigCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        EdgeSecurity edgeSecurity = environmentConfig.getEdgeSecurity();

        args.add(CreateCloudFrontLogProcessingLambdaConfigCommand.RATE_LIMIT_PER_MINUTE_LONG_ARG);
        args.add(edgeSecurity.getRateLimitPerMinute());
        args.add(CreateCloudFrontLogProcessingLambdaConfigCommand.RATE_LIMIT_VIOLATION_BLOCK_PERIOD_IN_MINUTES_LONG_ARG);
        args.add(edgeSecurity.getRateLimitViolationBlockPeriodInMinutes());

        if (StringUtils.isNotBlank(edgeSecurity.getGoogleAnalyticsTrackingId())) {
            args.add(CreateCloudFrontLogProcessingLambdaConfigCommand.GOOGLE_ANALYTICS_TRACKING_ID_LONG_ARG);
            args.add(edgeSecurity.getGoogleAnalyticsTrackingId());
        }

        if (StringUtils.isNotBlank(edgeSecurity.getSlackWebHookUrl())) {
            args.add(CreateCloudFrontLogProcessingLambdaConfigCommand.SLACK_WEB_HOOK_URL_LONG_ARG);
            args.add(edgeSecurity.getSlackWebHookUrl());
        }

        if (StringUtils.isNotBlank(edgeSecurity.getSlackIcon())) {
            args.add(CreateCloudFrontLogProcessingLambdaConfigCommand.SLACK_ICON_LONG_ARG);
            args.add(edgeSecurity.getSlackIcon());
        }

        return args;
    }

    private static List<String> getPublishLambdaCommandArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        List<String> args = new LinkedList<>();
        String lambdaName = null;
        for (int i = 0; i < passedArgs.length; i++) {
            if (StringUtils.equals(passedArgs[i],  PublishLambdaCommand.LAMBDA_NAME_LONG_ARG) && i < passedArgs.length - 1) {
                lambdaName = passedArgs[i+1];
            }
        }

        if (StringUtils.isBlank(lambdaName)) {
            return args;
        }

        args.add(PublishLambdaCommand.LAMBDA_NAME_LONG_ARG);
        args.add(lambdaName);
        args.add(PublishLambdaCommand.ARTIFACT_URL_LONG_ARG);

        switch (lambdaName.toUpperCase()) {
            case "CLOUD_FRONT_SG_GROUP_IP_SYNC":
                args.add(environmentConfig.getEdgeSecurity().getCloudfrontSecurityGroupIpSyncLambdaArtifactUrl());
                break;
            case "WAF":
                args.add(environmentConfig.getEdgeSecurity().getCloudfrontLambdaArtifactUrl());
                break;
            default:
                args.add("");
        }

        return args;
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

    private static List<String> getCreateGatewayClusterConfigCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        args.add(CreateGatewayClusterCommand.HOSTNAME_LONG_ARG);
        args.add(environmentConfig.getHostname());

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

    private static List<String> getPublishDashboardCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        Dashboard dashboard = environmentConfig.getDashboard();

        args.add(PublishDashboardCommand.ARTIFACT_URL_LONG_ARG);
        args.add(dashboard.getArtifactUrl());

        if ( StringUtils.isNotBlank(dashboard.getOverrideArtifactUrl()) ) {
            args.add(PublishDashboardCommand.OVERRIDE_ARTIFACT_URL_LONG_ARG);
            args.add(dashboard.getOverrideArtifactUrl());
        }

        return args;
    }

    private static List<String> getCreateConsulClusterCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        Consul component = environmentConfig.getConsul();
        addCommonStackArgs(environmentConfig, args, component);

        return args;
    }

    private static List<String> getCreateVaultClusterCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        Vault component = environmentConfig.getVault();
        addCommonStackArgs(environmentConfig, args, component);

        return args;
    }

    private static List<String> getCreateCmsClusterCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        ManagementService component = environmentConfig.getManagementService();
        addCommonStackArgs(environmentConfig, args, component);

        return args;
    }

    private static List<String> getCreateGatewayClusterCommandArgs(EnvironmentConfig environmentConfig) {
        List<String> args = new LinkedList<>();

        Gateway component = environmentConfig.getGateway();
        addCommonStackArgs(environmentConfig, args, component);
        args.add(CreateGatewayClusterCommand.HOSTNAME_LONG_ARG);
        args.add(environmentConfig.getHostname());
        args.add(CreateGatewayClusterCommand.HOSTED_ZONE_ID_LONG_ARG);
        args.add(environmentConfig.getHostedZoneId());

        return args;
    }

    private static void addCommonStackArgs(EnvironmentConfig environmentConfig, List<String> args, CerberusStack stack) {
        args.add(StackDelegate.AMI_ID_LONG_ARG);
        args.add(stack.getAmiId());
        args.add(StackDelegate.INSTANCE_SIZE_LONG_ARG);
        args.add(stack.getInstanceSize());
        args.add(StackDelegate.KEY_PAIR_NAME_LONG_ARG);
        args.add(stack.getKeyPairName());

        if (stack.getDesiredInstances() != null) {
            args.add(StackDelegate.DESIRED_INSTANCES_LONG_ARG);
            args.add(stack.getDesiredInstances());
        }
        if (stack.getMinInstances() != null) {
            args.add(StackDelegate.MIN_INSTANCES_LONG_ARG);
            args.add(stack.getMinInstances());
        }
        if (stack.getMaxInstances() != null) {
            args.add(StackDelegate.MAX_INSTANCES_LONG_ARG);
            args.add(stack.getMaxInstances());
        }

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
            case "consul":
                args.add(environmentConfig.getConsul().getCertPath());
                break;
            case "vault":
                args.add(environmentConfig.getVault().getCertPath());
                break;
            case "cms":
                args.add(environmentConfig.getManagementService().getCertPath());
                break;
            case "gateway":
                args.add(environmentConfig.getGateway().getCertPath());
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

        args.add(CreateBaseCommand.OWNER_EMAIL_LONG_ARG);
        args.add(config.getOwnerEmail());
        args.add(CreateBaseCommand.COST_CENTER_LONG_ARG);
        args.add(config.getCostCenter());
        args.add(CreateBaseCommand.ADMIN_ROLE_ARN_LONG_ARG);
        args.add(config.getAdminRoleArn());
        args.add(CreateBaseCommand.VPC_HOSTED_ZONE_NAME_LONG_ARG);
        args.add(config.getVpcHostedZoneName());

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
            case "consul":
                cerberusStack = environmentConfig.getConsul();
                break;
            case "vault":
                cerberusStack = environmentConfig.getVault();
                break;
            case "cms":
                cerberusStack = environmentConfig.getManagementService();
                break;
            case "gateway":
                cerberusStack = environmentConfig.getGateway();
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

    private static String getStackName(String[] passedArgs) {
        for (int i = 0; i < passedArgs.length; i++) {
            if (StringUtils.equals(passedArgs[i], STACK_NAME_KEY)) {
                  return passedArgs[i+1];
            }
        }
        return null;
    }
}
