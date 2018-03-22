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

import com.google.common.collect.Lists;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.command.audit.CreateAuditLoggingStackCommand;
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.certificates.GenerateAndRotateCertificatesCommand;
import com.nike.cerberus.command.certificates.RotateCertificatesCommand;
import com.nike.cerberus.command.core.InitializeEnvironmentCommand;
import com.nike.cerberus.command.composite.UpdateAllStackTagsCommand;
import com.nike.cerberus.command.rds.CreateDatabaseCommand;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.command.certificates.UploadCertificateFilesCommand;
import com.nike.cerberus.command.certificates.UploadCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.domain.input.ManagementServiceInput;
import com.nike.cerberus.domain.input.ManagementServiceRegionSpecificInput;
import com.nike.cerberus.domain.input.RegionSpecificConfigurationInput;
import com.nike.cerberus.domain.input.VpcAccessWhitelistInput;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

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
                if (i < passedArgs.length && !StringUtils.startsWith(passedArgs[i + 1], "-")) {
                    args.add(passedArgs[i + 1]);
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
        if (!StringUtils.isBlank(commandName)) {
            args.addAll(getArgsForCommand(environmentConfig, commandName, passedArgs));
        }

        // convert to string array and return
        return args.toArray(new String[args.size()]);
    }

    public static List<String> getArgsForCommand(EnvironmentConfig environmentConfig, String commandName, String[] passedArgs) {
        if (environmentConfig == null) {
            return Lists.newArrayList(passedArgs);
        }

        List<String> args = new LinkedList<>();
        switch (commandName) {
            case InitializeEnvironmentCommand.COMMAND_NAME:
                args = getInitializeEnvironmentCommandArgs(environmentConfig);
                break;
            case UploadCertificateFilesCommand.COMMAND_NAME:
                args = getUploadCertFilesCommandArgs(environmentConfig, passedArgs);
                break;
            case CreateCmsClusterCommand.COMMAND_NAME:
                args = getCreateCmsClusterCommandArgs(environmentConfig);
                break;
            case WhitelistCidrForVpcAccessCommand.COMMAND_NAME:
                args = getWhitelistCidrForVpcAccessCommandArgs(environmentConfig);
                break;
            case CreateCmsConfigCommand.COMMAND_NAME:
                args = getCreateCmsConfigCommandArgs(environmentConfig, passedArgs);
                break;
            case UpdateCmsConfigCommand.COMMAND_NAME:
                args = getCreateCmsConfigCommandArgs(environmentConfig, passedArgs);
                break;
            case CreateVpcCommand.COMMAND_NAME:
                args = getCreateVpcCommandArgs(environmentConfig);
                break;
            case CreateSecurityGroupsCommand.COMMAND_NAME:
                args = getCreateSecurityGroupsCommandArgs(environmentConfig);
                break;
            case CreateDatabaseCommand.COMMAND_NAME:
                args = getCreateDatabaseCommandArgs(environmentConfig);
                break;
            case CreateLoadBalancerCommand.COMMAND_NAME:
                args = getCreateLoadBalancerCommandArgs(environmentConfig);
                break;
            case CreateRoute53Command.COMMAND_NAME:
                args = getCreateRoute53CommandArgs(environmentConfig);
                break;
            case CreateWafCommand.COMMAND_NAME:
                args = getCreateWafCommandArgs(environmentConfig);
                break;
            case GenerateCertificateFilesCommand.COMMAND_NAME:
                args = getGenerateCertificatesCommandArgs(environmentConfig);
                break;
            case CreateEdgeDomainRecordCommand.COMMAND_NAME:
                args = getCreateEdgeDomainRecordCommandArgs(environmentConfig);
                break;
            case GenerateAndRotateCertificatesCommand.COMMAND_NAME:
                args = getGenerateCertificatesCommandArgs(environmentConfig);
                break;
            case RotateCertificatesCommand.COMMAND_NAME:
                args = getUploadCertFilesCommandArgs(environmentConfig, passedArgs);
                break;
            case CreateAuditLoggingStackCommand.COMMAND_NAME:
                args = getCreateAuditLoggingStackCommandArgs(environmentConfig);
                break;
            case UpdateAllStackTagsCommand.COMMAND_NAME:
                args = getUpdateAllStackTagsCommandArgs(environmentConfig);
                break;
            default:
                break;
        }

        LoggerFactory.getLogger("com.nike.cerberus.cli.EnvironmentConfigToArgsMapper")
                .debug("Mapped the following args from the provided YAML\n" + String.join("\n", args));

        return args;
    }

    private static List<String> getCreateAuditLoggingStackCommandArgs(EnvironmentConfig environmentConfig) {
        return ArgsBuilder.create()
                .addOption(CreateAuditLoggingStackCommand.ADMIN_ROLE_ARN_LONG_ARG, environmentConfig.getAdminRoleArn())
                .build();
    }

    private static List<String> getCreateEdgeDomainRecordCommandArgs(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create()
                .addOption(CreateEdgeDomainRecordCommand.BASE_DOMAIN_NAME_LONG_ARG, environmentConfig.getBaseDomainName())
                .addOption(CreateEdgeDomainRecordCommand.HOSTED_ZONE_ID_LONG_ARG, environmentConfig.getHostedZoneId());

        if (StringUtils.isNotBlank(environmentConfig.getEdgeDomainNameOverride())) {
            args.addOption(CreateEdgeDomainRecordCommand.EDGE_DOMAIN_NAME_OVERRIDE, environmentConfig.getEdgeDomainNameOverride());
        }

        return args.build();
    }

    private static List<String> getCreateCmsConfigCommandArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        ArgsBuilder args = ArgsBuilder.create();
        ManagementServiceInput managementService = environmentConfig.getManagementService();
        args.addOption(CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG, managementService.getAdminGroup());
        managementService.getProperties().forEach(property -> {
            args.addOption(CreateCmsConfigCommand.PROPERTY_SHORT_ARG, property);
        });

        if (Arrays.stream(passedArgs).anyMatch(s -> s.equals(UpdateCmsConfigCommand.OVERWRITE_LONG_ARG))) {
            args.addFlag(UpdateCmsConfigCommand.OVERWRITE_LONG_ARG);
        }

        return args.build();
    }

    private static List<String> getWhitelistCidrForVpcAccessCommandArgs(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();

        VpcAccessWhitelistInput vpcAccessWhitelist = environmentConfig.getVpcAccessWhitelist();

        vpcAccessWhitelist.getCidrs().forEach(cidr -> {
            args.addOption(WhitelistCidrForVpcAccessCommand.CIDR_LONG_ARG, cidr);
        });

        vpcAccessWhitelist.getPorts().forEach(port -> {
            args.addOption(WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, port);
        });

        return args.build();
    }

    private static List<String> getCreateCmsClusterCommandArgs(EnvironmentConfig config) {
        RegionSpecificConfigurationInput primaryRegion = config.getPrimaryRegionConfig();
        ManagementServiceRegionSpecificInput cmsConfig = primaryRegion.getManagementService().orElseThrow(() ->
                new RuntimeException("management service config not defined in primary region config"));

        return ArgsBuilder.create()
                .addOption(StackDelegate.AMI_ID_LONG_ARG, cmsConfig.getAmiId())
                .addOption(StackDelegate.INSTANCE_SIZE_LONG_ARG, cmsConfig.getInstanceSize())
                .addOption(StackDelegate.KEY_PAIR_NAME_LONG_ARG, cmsConfig.getKeyPairName())
                .addAll(getGlobalTags(config))
                .build();
    }

    private static List<String> getGlobalTags(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();
        environmentConfig.getGlobalTags().forEach((key, value) ->
                args.addDynamicProperty(TagParametersDelegate.TAG_SHORT_ARG, key, value)
        );
        return args.build();
    }

    private static List<String> getUploadCertFilesCommandArgs(EnvironmentConfig environmentConfig, String[] passedArgs) {
        return ArgsBuilder.create()
                .addOptionUsingPassedArgIfPresent(
                        UploadCertificateFilesCommandParametersDelegate.CERT_PATH_LONG_ARG,
                        environmentConfig.getCertificateDirectory(),
                        passedArgs
                )
                .build();
    }

    private static List<String> getInitializeEnvironmentCommandArgs(EnvironmentConfig config) {
        ArgsBuilder args = ArgsBuilder.create()
                .addAll(getGlobalTags(config))
                .addOption(InitializeEnvironmentCommand.ADMIN_ROLE_ARN_LONG_ARG, config.getAdminRoleArn())
                .addOption(InitializeEnvironmentCommand.PRIMARY_REGION, config.getPrimaryRegion());

        args.addFlag(InitializeEnvironmentCommand.REGION_LONG_ARG);
        config.getRegionSpecificConfiguration().forEach((region, data) -> {
            args.addFlag(region);
        });

        return args.build();
    }

    private static List<String> getCreateVpcCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
    }

    private static List<String> getCreateSecurityGroupsCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
    }

    private static List<String> getCreateDatabaseCommandArgs(EnvironmentConfig config) {
        ArgsBuilder args = ArgsBuilder.create();
        if (config.getPrimaryRegionConfig().getRds().isPresent()) {
            args.addOption(CreateDatabaseCommand.INSTANCE_CLASS_LONG_ARG,
                    config.getPrimaryRegionConfig().getRds().get().getSize());

            args.addOption(CreateDatabaseCommand.RESTORE_FROM_SNAPSHOT,
                    config.getPrimaryRegionConfig().getRds().get().getDbClusterIdentifier());
        }
        args.addAll(getGlobalTags(config));
        return args.build();
    }

    private static List<String> getCreateLoadBalancerCommandArgs(EnvironmentConfig config) {
        ArgsBuilder args = ArgsBuilder.create();
        if (StringUtils.isNotBlank(config.getLoadBalancerSslPolicyOverride())) {
            args.addOption(CreateLoadBalancerCommand.LOAD_BALANCER_SSL_POLICY_OVERRIDE_LONG_ARG,
                    config.getLoadBalancerSslPolicyOverride());
        }

        args.addAll(getGlobalTags(config));
        return args.build();
    }

    private static List<String> getCreateRoute53CommandArgs(EnvironmentConfig config) {
        ArgsBuilder args = ArgsBuilder.create()
                .addOption(CreateRoute53Command.BASE_DOMAIN_NAME_LONG_ARG, config.getBaseDomainName())
                .addOption(CreateRoute53Command.HOSTED_ZONE_ID_LONG_ARG, config.getHostedZoneId());


        if (StringUtils.isNotBlank(config.getOriginDomainNameOverride())) {
            args.addOption(CreateRoute53Command.ORIGIN_DOMAIN_NAME_OVERRIDE, config.getOriginDomainNameOverride());
        }

        if (config.getPrimaryRegionConfig().getLoadBalancerDomainNameOverride().isPresent()) {
            args.addOption(CreateRoute53Command.LOAD_BALANCER_DOMAIN_NAME_OVERRIDE,
                    config.getPrimaryRegionConfig().getLoadBalancerDomainNameOverride().orElse(null));
        }

        return args.build();
    }

    private static List<String> getCreateWafCommandArgs(EnvironmentConfig config) {
        return ArgsBuilder.create()
                .addAll(getGlobalTags(config))
                .build();
    }

    private static List<String> getGenerateCertificatesCommandArgs(EnvironmentConfig config) {
        ArgsBuilder args = ArgsBuilder.create()
                .addOption(GenerateCertificateFilesCommandParametersDelegate.BASE_DOMAIN_LONG_ARG, config.getBaseDomainName())
                .addOption(GenerateCertificateFilesCommandParametersDelegate.HOSTED_ZONE_ID_LONG_ARG, config.getHostedZoneId())
                .addOption(GenerateCertificateFilesCommandParametersDelegate.ACME_API_LONG_ARG, config.getAcmeApiUrl())
                .addOption(GenerateCertificateFilesCommandParametersDelegate.CONTACT_EMAIL_LONG_ARG, config.getAcmeContactEmail())
                .addOption(GenerateCertificateFilesCommandParametersDelegate.CERT_FOLDER_LONG_ARG, config.getCertificateDirectory());

        if (StringUtils.isNotBlank(config.getEdgeDomainNameOverride())) {
            args.addOption(GenerateCertificateFilesCommandParametersDelegate.EDGE_DOMAIN_NAME_OVERRIDE_LONG_ARG, config.getEdgeDomainNameOverride());
        }

        if (StringUtils.isNotBlank(config.getOriginDomainNameOverride())) {
            args.addOption(CreateRoute53Command.ORIGIN_DOMAIN_NAME_OVERRIDE, config.getOriginDomainNameOverride());
        }

        if (StringUtils.isNotBlank(config.getPrimaryRegionConfig().getLoadBalancerDomainNameOverride().orElse(null))) {
            args.addOption(GenerateCertificateFilesCommandParametersDelegate.LOAD_BALANCER_DOMAIN_NAME_OVERRIDE_LONG_ARG,
                    config.getPrimaryRegionConfig().getLoadBalancerDomainNameOverride().orElse(null));
        }

        if (config.isEnableLeCertFix()) {
            args.addFlag(GenerateCertificateFilesCommandParametersDelegate.ENABLE_LE_CERTFIX_LONG_ARG);
        }

        if (config.getAdditionalSubjectNames() != null) {
            config.getAdditionalSubjectNames().forEach(sn -> {
                args.addOption(GenerateCertificateFilesCommandParametersDelegate.SUBJECT_ALT_NAME_LONG_ARG, sn);
            });
        }

        return args.build();
    }

    private static List<String> getUpdateAllStackTagsCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
    }

    private static String getStackName(String[] passedArgs) {
        for (int i = 0; i < passedArgs.length; i++) {
            if (StringUtils.equals(passedArgs[i], STACK_NAME_KEY)) {
                return passedArgs[i + 1];
            }
        }
        return null;
    }
}
