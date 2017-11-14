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

package com.nike.cerberus.operation.cms;

import com.amazonaws.regions.Regions;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.cms.CreateCmsCmkCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.operation.Operation;
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
import java.util.Map;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.CMK_ARNS_KEY;

/**
 * Create CMKs in multiple regions for CMS to use.
 * <p>
 * We don't want this as part of the base CloudFormation YAML because we are operating on multiple regions.
 */
public class CreateCmsCmkOperation implements Operation<CreateCmsCmkCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;
    private final EnvironmentMetadata environmentMetadata;
    private final KmsService kmsService;

    @Inject
    public CreateCmsCmkOperation(final ConfigStore configStore,
                                 final EnvironmentMetadata environmentMetadata,
                                 final KmsService kmsService) {
        this.configStore = configStore;
        this.environmentMetadata = environmentMetadata;
        this.kmsService = kmsService;
    }

    @Override
    public void run(final CreateCmsCmkCommand command) {
        // load the existing configuration
        logger.info("Retrieving configuration data from the configuration bucket.");
        final Properties cmsConfigProperties = configStore.getAllExistingCmsEnvProperties();

        // create the CMKs
        List<String> cmkArns = createCmks(command, cmsConfigProperties);

        // store the new configuration
        cmsConfigProperties.put(CMK_ARNS_KEY, StringUtils.join(cmkArns, ","));
        logger.info("Uploading the CMS configuration to the configuration bucket.");
        configStore.storeCmsEnvConfig(cmsConfigProperties);
        logger.info("Uploading complete.");
    }

    @Override
    public boolean isRunnable(final CreateCmsCmkCommand command) {
        boolean isRunnable = configStore.getCmsEnvConfig().isPresent();

        if (!isRunnable) {
            logger.warn("CMS config does not exist, please use 'create-cms-config' command first.");
        }

        try {
            final Properties cmsConfigProperties = configStore.getAllExistingCmsEnvProperties();
            // additional argument validation
            validateRegionsArg(command);
            validateRotateArg(command, cmsConfigProperties);
        } catch (ParameterException e) {
            return false;
        }


        return isRunnable;
    }

    private void validateRotateArg(CreateCmsCmkCommand command, Properties cmsConfigProperties) {
        if (command.isRotate()) {
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

    private void validateRegionsArg(CreateCmsCmkCommand command) {
        String primaryRegion = environmentMetadata.getRegionName();
        List<String> regions = command.getAdditionalRegions();
        if (regions.contains(primaryRegion)) {
            throw new ParameterException("Additional regions should not contain the primary region for the environment");
        }
    }

    private List<String> createCmks(CreateCmsCmkCommand command, Properties cmsConfigProperties) {
        String envName = environmentMetadata.getName();
        String primaryRegion = environmentMetadata.getRegionName();

        String alias = generateAliasName(envName, primaryRegion);

        String description = "Generated for the Cerberus " + envName + " environment running in " + primaryRegion;
        String policyAsJson = generateKeyPolicy(cmsConfigProperties, description);
        logger.info("Generated the following policy:\n" + policyAsJson);

        Map<String,String> tags = Maps.newHashMap();
        tags.put("created_by", "cerberus_cli");
        tags.put("created_for", "cerberus_cms");
        tags.put("cerberus_env", envName);

        List<Regions> cmkRegions = getTargetRegions(command);
        return kmsService.createKeysAndAliases(cmkRegions, alias, policyAsJson, description, tags);
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

    private List<Regions> getTargetRegions(CreateCmsCmkCommand command) {
        // make sure primary region is first in the list
        List<Regions> cmkRegions = Lists.newArrayList(environmentMetadata.getRegions());
        for (String region : command.getAdditionalRegions()) {
            cmkRegions.add(Regions.fromName(region));
        }
        return cmkRegions;
    }
}
