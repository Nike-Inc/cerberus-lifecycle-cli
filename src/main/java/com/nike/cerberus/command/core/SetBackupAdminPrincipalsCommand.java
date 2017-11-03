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

package com.nike.cerberus.command.core;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nike.cerberus.command.core.SetBackupAdminPrincipalsCommand.COMMAND_NAME;
import static com.nike.cerberus.module.CerberusModule.getAWSCredentialsProviderChain;

/**
 * Command to update which principals besides for the root account will have permissions to use the backup cmk,
 * AKA create and restore backups.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Update the IAM Principals that are allowed to create and restore backups. " +
                "This command automatically adds by default the root user and configured admin user arn, " +
                "but you can use this command to add iam principals such as CI systems and additional user principals " +
                "that will have access to encrypt and decrypt backup data"
)
public class SetBackupAdminPrincipalsCommand implements Command {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String COMMAND_NAME = "set-backup-principals";

    private static final String AWS_PROVIDER = "AWS";

    public static final String PRINCIPAL_LONG_ARG = "--principal";

    public static final String PRINCIPAL_SHORT_ARG = "-p";

    @Parameter(names = { PRINCIPAL_LONG_ARG, PRINCIPAL_SHORT_ARG },
            description = "One or more additional principals to grant access to.")
    private List<String> additionalPrincipals = new ArrayList<>();

    private final ConfigStore configStore;

    private final AWSSecurityTokenService sts;

    @Inject
    public SetBackupAdminPrincipalsCommand(ConfigStore configStore,
                                             AWSSecurityTokenService sts) {

        this.configStore = configStore;
        this.sts = sts;
    }

    @Override
    public void execute() {
        GetCallerIdentityResult identityResult = sts.getCallerIdentity(new GetCallerIdentityRequest());
        String accountId = identityResult.getAccount();
        String rootArn = String.format("arn:aws:iam::%s:root", accountId);
        String adminRoleArn = configStore.getAccountAdminArn().get();

        Set<String> principals = new HashSet<>();
        principals.add(rootArn);
        principals.add(adminRoleArn);
        principals.addAll(additionalPrincipals);

        configStore.storeBackupAdminIamPrincipals(principals);

        if (! configStore.getRegionBackupBucketMap().isEmpty()) {
            configStore.getRegionBackupBucketMap().forEach((region, backupRegionInfo) -> {
                final List<Statement> statements = new LinkedList<>();
                principals.forEach( principal -> {
                    log.debug("Adding principal: {} to the CMK Policy for region {}", principal, region);
                    statements.add(new Statement(Statement.Effect.Allow)
                            .withId("Principal " + principal + " Has All Actions")
                            .withPrincipals(new Principal(AWS_PROVIDER, principal, false))
                            .withActions(KMSActions.AllKMSActions)
                            .withResources(new Resource("*")));
                });

                Policy kmsPolicy = new Policy();
                kmsPolicy.setStatements(statements);
                String policyString = kmsPolicy.toJson();

                log.debug("Updating key {} for region {} with policy {}", backupRegionInfo.getKmsCmkId(), region, policyString);

                AWSKMS kms = AWSKMSClient.builder().withCredentials(getAWSCredentialsProviderChain()).withRegion(region).build();
                PutKeyPolicyRequest request = new PutKeyPolicyRequest()
                        .withKeyId(backupRegionInfo.getKmsCmkId())
                        .withPolicyName("default")
                        .withBypassPolicyLockoutSafetyCheck(true)
                        .withPolicy(policyString);

                kms.putKeyPolicy(request);

                log.info("Successfully updated key {} in region {} to allow the following principals access {}",
                        backupRegionInfo.getKmsCmkId(), region, String.join(", ", principals));
            });
        }
    }

    @Override
    public boolean isRunnable() {
        Optional<String> adminIamPrincipalArn = configStore.getAccountAdminArn();
        if (! adminIamPrincipalArn.isPresent()) {
            log.error("The admin IAM principal must be set for this environment");
            return false;
        }
        return true;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }
}
