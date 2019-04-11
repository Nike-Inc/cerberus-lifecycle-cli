/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.operation.certificates;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.certificates.RotateCertificatesCommand;
import com.nike.cerberus.command.certificates.DeleteOldestCertificatesCommand;
import com.nike.cerberus.command.core.RebootCmsCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.certificates.UploadCertificateFilesCommand;
import com.nike.cerberus.command.certificates.UploadCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.operation.composite.ChainableCommand;
import com.nike.cerberus.operation.composite.CompositeOperation;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.util.List;

import static com.nike.cerberus.command.certificates.DeleteOldestCertificatesCommandParametersDelegate.ACME_API_LONG_ARG;
import static com.nike.cerberus.command.certificates.DeleteOldestCertificatesCommandParametersDelegate.REVOKE_LONG_ARG;
import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Operation for the certificate rotation command
 */
public class RotateCertificatesOperation extends CompositeOperation<RotateCertificatesCommand> {

    private final CloudFormationService cloudFormationService;
    private final String environmentName;
    private final ConfigStore configStore;

    @Inject
    public RotateCertificatesOperation(CloudFormationService cloudFormationService,
                                       @Named(ENV_NAME) String environmentName,
                                       ConfigStore configStore) {

        this.cloudFormationService = cloudFormationService;
        this.environmentName = environmentName;
        this.configStore = configStore;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain(RotateCertificatesCommand compositeCommand) {
        ChainableCommand.Builder delete = ChainableCommand.Builder.create().withCommand(new DeleteOldestCertificatesCommand());
        if (compositeCommand.getDeleteParametersDelegate().isRevokeCertificates()) {
            delete
                    .withAdditionalArg(REVOKE_LONG_ARG)
                    .withOption(ACME_API_LONG_ARG, compositeCommand.getDeleteParametersDelegate().getAcmeUrl());
        }

        return Lists.newArrayList(
                // Add the cert and key files to S3
                ChainableCommand.Builder.create().withCommand(new UploadCertificateFilesCommand())
                        .withAdditionalArg(UploadCertificateFilesCommandParametersDelegate.CERT_PATH_LONG_ARG)
                        .withAdditionalArg(compositeCommand.getUploadParametersDelegate()
                                .getCertPath().toString())
                        .build(),

                // Update the Load Balancer stack to use the new cert
                ChainableCommand.Builder.create().withCommand(new UpdateStackCommand())
                        .withAdditionalArg(UpdateStackCommand.STACK_NAME_LONG_ARG)
                        .withAdditionalArg(Stack.LOAD_BALANCER.getName())
                        .build(),

                // Generate new CMS config that points to the new cert
                ChainableCommand.Builder.create().withCommand(new UpdateCmsConfigCommand()).build(),

                // Do a rolling reboot of the management service
                ChainableCommand.Builder.create().withCommand(new RebootCmsCommand()).build(),

                // Delete all certs except the latest (there should just be the one)
                delete.build()
        );
    }

    @Override
    public boolean isRunnable(RotateCertificatesCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.LOAD_BALANCER.getFullName(environmentName))) {
            log.error("The load-balancer stack must be present in order to rotate certificates");
            isRunnable = false;
        }

        if (! cloudFormationService.isStackPresent(configStore.getPrimaryRegion(), Stack.CMS.getFullName(environmentName))) {
            log.error("The cms stack must be present to rotate certificates");
            isRunnable = false;
        }

        if (command.getDeleteParametersDelegate().isRevokeCertificates() &&
                StringUtils.isBlank(command.getDeleteParametersDelegate().getAcmeUrl())) {
            log.error("You must provide an ACME api url if you wish to revoke the cert while rotating");
            isRunnable = false;
        }

        return isRunnable;
    }

    @Override
    public boolean isEnvironmentConfigRequired() {
        return false;
    }
}
