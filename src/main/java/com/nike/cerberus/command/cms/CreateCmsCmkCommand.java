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

package com.nike.cerberus.command.cms;

import com.amazonaws.regions.Regions;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.service.KmsPolicyGenerator;
import com.nike.cerberus.service.KmsService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.CMK_ARNS_KEY;
import static com.nike.cerberus.command.cms.UpdateCmsConfigCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the CMS config.")
public class CreateCmsCmkCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "create-cms-cmk";

    public static final String ADDITIONAL_REGIONS_ARG = "--additional-regions";

    public static final String ROTATE_ARG = "--rotate";

    @Parameter(names = ADDITIONAL_REGIONS_ARG,
            description = "Additional regions to create the CMS CMK in. At least one additional region, other than the primary region where Cerberus will be running, is required.",
            variableArity = true,
            required = true
    )
    private List<String> additionalRegions;

    @Parameter(names = ROTATE_ARG,
            description = "Manually rotates the CMKs while leaving the old CMKs in place so they can still be used. " +
                    "This is generally unnecessary since AWS will automatically rotate keys previously created with this command. "
    )
    private boolean rotate = false;

    private final ConfigStore configStore;

    private final EnvironmentMetadata environmentMetadata;

    private final KmsService kmsService;

    @Inject
    public CreateCmsCmkCommand(final ConfigStore configStore,
                               final EnvironmentMetadata environmentMetadata,
                               final KmsService kmsService) {
        this.configStore = configStore;
        this.environmentMetadata = environmentMetadata;
        this.kmsService = kmsService;
    }

    @Override
    public void execute() {
        // load the existing configuration
        logger.info("Retrieving configuration data from the configuration bucket.");
        final Properties cmsConfigProperties = configStore.getAllExistingCmsEnvProperties();

        // additional argument validation
        validateRegionsArg();
        validateRotateArg(cmsConfigProperties);

        String envName = environmentMetadata.getName();
        String primaryRegion = environmentMetadata.getRegionName();

        String alias = generateAliasName(envName, primaryRegion);

        String description = "Generated for the Cerberus " + envName + " environment running in " + primaryRegion;
        String policyAsJson = generateKeyPolicy(cmsConfigProperties, description);
        logger.info("Generated the following policy:\n" + policyAsJson);

        List<Regions> cmkRegions = getTargetRegions();
        List<String> cmkArns = kmsService.createKeysAndAliases(cmkRegions, alias, policyAsJson, description);

        // store the new configuration
        cmsConfigProperties.put(CMK_ARNS_KEY, StringUtils.join(cmkArns, ","));
        logger.info("Uploading the CMS configuration to the configuration bucket.");
        configStore.storeCmsEnvConfig(cmsConfigProperties);
        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable() {
        boolean isRunnable = configStore.getCmsEnvConfig().isPresent();

        if (!isRunnable) {
            logger.warn("CMS config does not exist, please use 'create-cms-config' command first.");
        }

        return isRunnable;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    private void validateRotateArg(Properties cmsConfigProperties) {
        if (rotate) {
            if (cmsConfigProperties.containsKey(CMK_ARNS_KEY)) {
                logger.info("Overwriting existing CMK ARNs property: " + cmsConfigProperties.getProperty(CMK_ARNS_KEY));
                logger.info("Existing CMKs will not be deleted and may be needed to decrypt existing secrets");
            } else {
                throw new ParameterException(CreateCmsCmkCommand.ROTATE_ARG + " was specified but there is no existing CMK to rotate");
            }
        } else {
            if (cmsConfigProperties.containsKey(CMK_ARNS_KEY)) {
                logger.info("Property already exists: " + CMK_ARNS_KEY + "=" + cmsConfigProperties.get(CMK_ARNS_KEY));
                throw new ParameterException("CMK already exists but " + CreateCmsCmkCommand.ROTATE_ARG + " was not specified.  Generally, manual rotation is not necessary.");
            } else {
                logger.info("Will add CMK ARNs property: " + CMK_ARNS_KEY);
            }
        }
    }

    private void validateRegionsArg() {
        String primaryRegion = environmentMetadata.getRegionName();
        List<String> regions = additionalRegions;
        if (regions.contains(primaryRegion)) {
            throw new ParameterException("Additional regions should not contain the primary region for the environment");
        }
    }

    private String generateAliasName(String envName, String primaryRegion) {
        return "alias/cerberus/cms-" + envName + "-" + primaryRegion + "-" + DateFormatUtils.format(new Date(), "yyyy-MM-dd-HH-mm");
    }

    private String generateKeyPolicy(Properties cmsConfigProperties, String description) {
        String rootUserArn = getRequiredProperty(cmsConfigProperties, ConfigConstants.ROOT_USER_ARN_KEY);
        String adminUserArn = getRequiredProperty(cmsConfigProperties, ConfigConstants.ADMIN_ROLE_ARN_KEY);
        String cmsRoleArn = getRequiredProperty(cmsConfigProperties, ConfigConstants.CMS_ROLE_ARN_KEY);

        KmsPolicyGenerator generator = new KmsPolicyGenerator()
                .withDescription(description)
                .withAdminArns(Lists.newArrayList(rootUserArn, adminUserArn))
                .withCmsArn(cmsRoleArn);

        return generator.generatePolicyJson();
    }

    private String getRequiredProperty(Properties cmsConfigProperties, String propertyName) {
        String value = cmsConfigProperties.getProperty(propertyName);
        Validate.notBlank(value, "CMS config value " + propertyName + " was not found!");
        return value;
    }

    private List<Regions> getTargetRegions() {
        // make sure primary region is first in the list
        List<Regions> cmkRegions = Lists.newArrayList(environmentMetadata.getRegions());
        for (String region : additionalRegions) {
            cmkRegions.add(Regions.fromName(region));
        }
        return cmkRegions;
    }
}
