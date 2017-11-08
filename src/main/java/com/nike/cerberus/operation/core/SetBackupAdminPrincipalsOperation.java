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

package com.nike.cerberus.operation.core;

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
import com.nike.cerberus.command.core.SetBackupAdminPrincipalsCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.nike.cerberus.module.CerberusModule.getAWSCredentialsProviderChain;

/**
 * Operation to update which principals besides for the root account will have permissions to use the backup cmk,
 * AKA create and restore backups
 */
public class SetBackupAdminPrincipalsOperation implements Operation<SetBackupAdminPrincipalsCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String AWS_PROVIDER = "AWS";

    private final ConfigStore configStore;

    private final AWSSecurityTokenService sts;

    @Inject
    public SetBackupAdminPrincipalsOperation(ConfigStore configStore,
                                             AWSSecurityTokenService sts) {

        this.configStore = configStore;
        this.sts = sts;
    }

    @Override
    public void run(SetBackupAdminPrincipalsCommand command) {
        GetCallerIdentityResult identityResult = sts.getCallerIdentity(new GetCallerIdentityRequest());
        String accountId = identityResult.getAccount();
        String rootArn = String.format("arn:aws:iam::%s:root", accountId);
        String adminRoleArn = configStore.deprecatedGetAccountAdminArn().get();

        Set<String> principals = new HashSet<>();
        principals.add(rootArn);
        principals.add(adminRoleArn);
        principals.addAll(command.getAdditionalPrincipals());

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
    public boolean isRunnable(SetBackupAdminPrincipalsCommand command) {
        Optional<String> adminIamPrincipalArn = configStore.deprecatedGetAccountAdminArn();
        if (! adminIamPrincipalArn.isPresent()) {
            log.error("The admin IAM principal must be set for this environment");
            return false;
        }
        return true;
    }

}
