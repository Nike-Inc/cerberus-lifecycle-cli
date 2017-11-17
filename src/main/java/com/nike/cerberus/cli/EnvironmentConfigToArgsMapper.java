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
import com.nike.cerberus.command.cms.CreateCmsCmkCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.CreateDatabaseCommand;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.UploadCertificateFilesCommand;
import com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;
import com.nike.cerberus.domain.input.EnvironmentConfig;
import com.nike.cerberus.domain.input.ManagementService;
import com.nike.cerberus.domain.input.VpcAccessWhitelist;
import org.apache.commons.lang3.StringUtils;

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
        switch (commandName) {
            case CreateBaseCommand.COMMAND_NAME:
                return getCreateBaseCommandArgs(environmentConfig);
            case UploadCertificateFilesCommand.COMMAND_NAME:
                return getUploadCertFilesCommandArgs(environmentConfig);
            case CreateCmsClusterCommand.COMMAND_NAME:
                return getCreateCmsClusterCommandArgs(environmentConfig);
            case WhitelistCidrForVpcAccessCommand.COMMAND_NAME:
                return getWhitelistCidrForVpcAccessCommandArgs(environmentConfig);
            case CreateCmsConfigCommand.COMMAND_NAME:
                return getCreateCmsConfigCommandArgs(environmentConfig);
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
            case GenerateCertificateFilesCommand.COMMAND_NAME:
                return getGenerateCertificatesCommandArgs(environmentConfig);
            case CreateCmsCmkCommand.COMMAND_NAME:
                return getCreateCmsCmkCommandArgs(environmentConfig);
            case CreateEdgeDomainRecordCommand.COMMAND_NAME:
                return getCreateEdgeDomainRecordCommandArgs(environmentConfig);
            default:
                return new LinkedList<>();
        }
    }

    private static List<String> getCreateEdgeDomainRecordCommandArgs(EnvironmentConfig environmentConfig) {
        return ArgsBuilder.create()
                .addOption(CreateEdgeDomainRecordCommand.BASE_DOMAIN_NAME_LONG_ARG, environmentConfig.getBaseDomainName())
                .addOption(CreateEdgeDomainRecordCommand.HOSTED_ZONE_ID_LONG_ARG, environmentConfig.getHostedZoneId())
                .addOption(CreateEdgeDomainRecordCommand.EDGE_DOMAIN_NAME_OVERRIDE, environmentConfig.getEdgeDomainNameOverride())
                .build();
    }

    private static List<String> getCreateCmsCmkCommandArgs(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();

        if (environmentConfig.getManagementService().getAdditionalEncryptionCmkRegions() != null
                && environmentConfig.getManagementService().getAdditionalEncryptionCmkRegions().size() >= 1) {
            environmentConfig.getManagementService().getAdditionalEncryptionCmkRegions().forEach(region -> {
                args.addOption(CreateCmsCmkCommand.ADDITIONAL_REGIONS_ARG, region);
            });
        } else {
            throw new RuntimeException(String.format("%s requires at least 1 additional region be specified for high " +
                    "availability, add at least 1 region to 'encryption-cmk-regions'", CreateCmsCmkCommand.COMMAND_NAME));
        }

        return args.build();
    }

    private static List<String> getCreateCmsConfigCommandArgs(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();
        ManagementService managementService = environmentConfig.getManagementService();
        args.addOption(CreateCmsConfigCommand.ADMIN_GROUP_LONG_ARG, managementService.getAdminGroup());
        managementService.getProperties().forEach(property -> {
            args.addOption(CreateCmsConfigCommand.PROPERTY_SHORT_ARG, property);
        });
        return args.build();
    }

    private static List<String> getWhitelistCidrForVpcAccessCommandArgs(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();

        VpcAccessWhitelist vpcAccessWhitelist = environmentConfig.getVpcAccessWhitelist();

        vpcAccessWhitelist.getCidrs().forEach(cidr -> {
            args.addOption(WhitelistCidrForVpcAccessCommand.CIDR_LONG_ARG, cidr);
        });

        vpcAccessWhitelist.getPorts().forEach(port -> {
            args.addOption(WhitelistCidrForVpcAccessCommand.PORT_LONG_ARG, port);
        });

        return args.build();
    }

    private static List<String> getCreateCmsClusterCommandArgs(EnvironmentConfig environmentConfig) {
        return ArgsBuilder.create()
                .addOption(StackDelegate.AMI_ID_LONG_ARG, environmentConfig.getManagementService().getAmiId())
                .addOption(StackDelegate.INSTANCE_SIZE_LONG_ARG, environmentConfig.getManagementService().getInstanceSize())
                .addOption(StackDelegate.KEY_PAIR_NAME_LONG_ARG, environmentConfig.getManagementService().getKeyPairName())
                .addAll(getGlobalTags(environmentConfig))
                .build();
    }

    private static List<String> getGlobalTags(EnvironmentConfig environmentConfig) {
        ArgsBuilder args = ArgsBuilder.create();
        environmentConfig.getGlobalTags().forEach((key, value) ->
                args.addDynamicProperty(TagParametersDelegate.TAG_SHORT_ARG, key, value)
        );
        return args.build();
    }

    private static List<String> getUploadCertFilesCommandArgs(EnvironmentConfig environmentConfig) {
        return ArgsBuilder.create()
                .addOption(UploadCertificateFilesCommand.CERT_PATH_LONG_ARG,
                        environmentConfig.getManagementService().getCertPath())
                .build();
    }

    private static List<String> getCreateBaseCommandArgs(EnvironmentConfig config) {
        return ArgsBuilder.create()
                .addAll(getGlobalTags(config))
                .addOption(CreateBaseCommand.ADMIN_ROLE_ARN_LONG_ARG, config.getAdminRoleArn())
                .build();
    }

    private static List<String> getCreateVpcCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
    }

    private static List<String> getCreateSecurityGroupsCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
    }

    private static List<String> getCreateDatabaseCommandArgs(EnvironmentConfig config) {
        return getGlobalTags(config);
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
        return ArgsBuilder.create()
                .addOption(CreateRoute53Command.BASE_DOMAIN_NAME_LONG_ARG, config.getBaseDomainName())
                .addOption(CreateRoute53Command.HOSTED_ZONE_ID_LONG_ARG, config.getHostedZoneId())
                .addOption(CreateRoute53Command.ORIGIN_DOMAIN_NAME_OVERRIDE, config.getOriginDomainNameOverride())
                .addOption(CreateRoute53Command.LOAD_BALANCER_DOMAIN_NAME_OVERRIDE, config.getLoadBalancerDomainNameOverride())
                .build();
    }

    private static List<String> getCreateWafCommandArgs(EnvironmentConfig config) {
        return ArgsBuilder.create()
                .addAll(getGlobalTags(config))
                .build();
    }

    private static List<String> getGenerateCertificatesCommandArgs(EnvironmentConfig config) {
        ArgsBuilder argsBuilder = ArgsBuilder.create()
                .addOption(GenerateCertificateFilesCommand.BASE_DOMAIN_LONG_ARG, config.getBaseDomainName())
                .addOption(GenerateCertificateFilesCommand.EDGE_DOMAIN_NAME_OVERRIDE_LONG_ARG, config.getEdgeDomainNameOverride())
                .addOption(GenerateCertificateFilesCommand.ORIGIN_DOMAIN_NAME_OVERRIDE_LONG_ARG, config.getOriginDomainNameOverride())
                .addOption(GenerateCertificateFilesCommand.LOAD_BALANCER_DOMAIN_NAME_OVERRIDE_LONG_ARG, config.getLoadBalancerDomainNameOverride())
                .addOption(GenerateCertificateFilesCommand.HOSTED_ZONE_ID_LONG_ARG, config.getHostedZoneId())
                .addOption(GenerateCertificateFilesCommand.ACME_API_LONG_ARG, config.getAcmeApiUrl())
                .addOption(GenerateCertificateFilesCommand.CONTACT_EMAIL_LONG_ARG, config.getAcmeContactEmail())
                .addOption(GenerateCertificateFilesCommand.CERT_FOLDER_LONG_ARG, config.getLocalFolderToStoreCerts());

        if (config.isEnableLeCertFix()) {
            argsBuilder.addFlag(GenerateCertificateFilesCommand.ENABLE_LE_CERTFIX_LONG_ARG);
        }

        if (config.getAdditionalSubjectNames() != null) {
            config.getAdditionalSubjectNames().forEach(sn -> {
                argsBuilder.addOption(GenerateCertificateFilesCommand.SUBJECT_ALT_NAME_LONG_ARG, sn);
            });
        }

        return argsBuilder.build();
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
