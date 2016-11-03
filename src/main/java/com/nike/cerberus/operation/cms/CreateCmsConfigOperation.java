package com.nike.cerberus.operation.cms;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.collect.Maps;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.domain.cloudformation.BaseOutputs;
import com.nike.cerberus.domain.cloudformation.BaseParameters;
import com.nike.cerberus.domain.cloudformation.VaultParameters;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * Gathers all of the CMS environment configuration and puts it in the configu bucket.
 */
public class CreateCmsConfigOperation implements Operation<CreateCmsConfigCommand> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private final AWSSecurityTokenService securityTokenService;

    @Inject
    public CreateCmsConfigOperation(final ConfigStore configStore,
                                    final AWSSecurityTokenService securityTokenService) {
        this.configStore = configStore;
        this.securityTokenService = securityTokenService;
    }

    @Override
    public void run(final CreateCmsConfigCommand command) {
        configStore.storeCmsAdminGroup(command.getAdminGroup());

        final BaseOutputs baseOutputs = configStore.getBaseStackOutputs();
        final BaseParameters baseParameters = configStore.getBaseStackParameters();
        final VaultParameters vaultParameters = configStore.getVaultStackParamters();
        final GetCallerIdentityResult callerIdentity = securityTokenService.getCallerIdentity(
                new GetCallerIdentityRequest());
        final Optional<String> cmsVaultToken = configStore.getCmsVaultToken();
        final Optional<String> cmsDatabasePassword = configStore.getCmsDatabasePassword();

        final Map<String, String> cmsConfigMap = Maps.newHashMap();
        final String rootUserArn = String.format("arn:aws:iam::%s:root", callerIdentity.getAccount());

        cmsConfigMap.put("vault.addr", String.format("https://%s", cnameToHost(vaultParameters.getCname())));
        cmsConfigMap.put("vault.token", cmsVaultToken.get());
        cmsConfigMap.put("cms.admin.group", command.getAdminGroup());
        cmsConfigMap.put("cms.jdbc.jdbcUrl", baseOutputs.getCmsDbJdbcConnectionString());
        cmsConfigMap.put("cms.jdbc.jdbcUser", ConfigConstants.DEFAULT_CMS_DB_NAME);
        cmsConfigMap.put("cms.jdbc.jdbcPassword", cmsDatabasePassword.get());
        cmsConfigMap.put("root.user.arn", rootUserArn);
        cmsConfigMap.put("admin.role.arn", baseParameters.getAccountAdminArn());
        cmsConfigMap.put("cms.role.arn", baseOutputs.getCmsIamRoleArn());

        configStore.storeCmsEnvConfig(cmsConfigMap);
    }

    @Override
    public boolean isRunnable(final CreateCmsConfigCommand command) {
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
