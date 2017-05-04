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

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.VaultParameters;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static com.nike.cerberus.ConfigConstants.ADMIN_ROLE_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.SYSTEM_CONFIGURED_PROPERTIES;
import static com.nike.cerberus.ConfigConstants.CMS_ADMIN_GROUP_KEY;
import static com.nike.cerberus.ConfigConstants.CMS_ROLE_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.JDBC_PASSWORD_KEY;
import static com.nike.cerberus.ConfigConstants.JDBC_URL_KEY;
import static com.nike.cerberus.ConfigConstants.JDBC_USERNAME_KEY;
import static com.nike.cerberus.ConfigConstants.ROOT_USER_ARN_KEY;
import static com.nike.cerberus.ConfigConstants.VAULT_ADDR_KEY;
import static com.nike.cerberus.ConfigConstants.VAULT_TOKEN_KEY;

/**
 * Gathers all of the CMS environment configuration and puts it in the config bucket.
 */
public class UpdateCmsConfigOperation implements Operation<UpdateCmsConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final AWSSecurityTokenService securityTokenService;

    @Inject
    public UpdateCmsConfigOperation(final ConfigStore configStore,
                                    final AWSSecurityTokenService securityTokenService) {
        this.configStore = configStore;
        this.securityTokenService = securityTokenService;
    }

    @Override
    public void run(final UpdateCmsConfigCommand command) {

        logger.debug("Retrieving configuration data from the configuration bucket.");
        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final BaseParameters baseParameters = configStore.getBaseStackParameters();
        final VaultParameters vaultParameters = configStore.getVaultStackParamters();
        final GetCallerIdentityResult callerIdentity = securityTokenService.getCallerIdentity(
                new GetCallerIdentityRequest());
        final Optional<String> cmsVaultToken = configStore.getCmsVaultToken();
        final Optional<String> cmsDatabasePassword = configStore.getCmsDatabasePassword();
        final String rootUserArn = String.format("arn:aws:iam::%s:root", callerIdentity.getAccount());
        final String adminGroupParameter = command.getAdminGroup();
        final boolean overwriteCustomProperties = command.getOverwrite();

        final Properties newProperties = new Properties();
        final Properties existingProperties = configStore.getCmsEnvProperties();
        final String existingAdminGroup = existingProperties.getProperty(CMS_ADMIN_GROUP_KEY);
        if (! overwriteCustomProperties) {
            // keep already existing custom properties
            newProperties.putAll(existingProperties);
        } else if (adminGroupParameter == null) {
            // admin group is required for overwrite
            throw new IllegalStateException("Please provide admin group with overwrite flag.");
        } else {
            // admin group may not be set in this case, so set it
            newProperties.put(CMS_ADMIN_GROUP_KEY, existingAdminGroup == null ? adminGroupParameter : existingAdminGroup);
        }

        // retrieve generated properties
        newProperties.put(VAULT_ADDR_KEY, String.format("https://%s", cnameToHost(vaultParameters.getCname())));
        newProperties.put(VAULT_TOKEN_KEY, cmsVaultToken.get());
        newProperties.put(ROOT_USER_ARN_KEY, rootUserArn);
        newProperties.put(ADMIN_ROLE_ARN_KEY, baseParameters.getAccountAdminArn());
        newProperties.put(CMS_ROLE_ARN_KEY, baseOutputs.getCmsIamRoleArn());
        newProperties.put(JDBC_URL_KEY, baseOutputs.getCmsDbJdbcConnectionString());
        newProperties.put(JDBC_USERNAME_KEY, ConfigConstants.DEFAULT_CMS_DB_NAME);
        newProperties.put(JDBC_PASSWORD_KEY, cmsDatabasePassword.get());

        // only update if necessary
        if (adminGroupParameter != null && !StringUtils.equals(adminGroupParameter, existingAdminGroup)) {
            logger.warn(String.format("Updating CMS admin group from '%s' to '%s'", existingAdminGroup, adminGroupParameter));
            configStore.storeCmsAdminGroup(adminGroupParameter);
            newProperties.put(CMS_ADMIN_GROUP_KEY, adminGroupParameter);
        }

        command.getAdditionalProperties().forEach((k, v) -> {
            if (! SYSTEM_CONFIGURED_PROPERTIES.contains(k)) {
                newProperties.put(k, v);
            } else {
                logger.warn("Ignoring additional property that would override system configured property, " + k);
            }
        });

        configStore.storeCmsEnvConfig(newProperties);
    }

    @Override
    public boolean isRunnable(final UpdateCmsConfigCommand command) {
        boolean isRunnable = true;
        final Optional<String> cmsVaultToken = configStore.getCmsVaultToken();
        final Optional<String> cmsDatabasePassword = configStore.getCmsDatabasePassword();

        if (!cmsVaultToken.isPresent()) {
            logger.error("CMS Vault token not present for specified environment.");
            isRunnable = false;
        }

        if (!cmsDatabasePassword.isPresent()) {
            logger.error("CMS database password not present for specified environment.");
            isRunnable = false;
        }

        return isRunnable;
    }

    /**
     * Removes the final '.' from the CNAME.
     *
     * @param cname The cname to convert
     * @return The host derived from the CNAME
     */
    private String cnameToHost(final String cname) {
        return cname.substring(0, cname.length() - 1);
    }
}
