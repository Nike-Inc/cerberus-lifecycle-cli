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

package com.nike.cerberus.operation.composite;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.nike.cerberus.command.cms.UpdateCmsConfigCommand;
import com.nike.cerberus.command.composite.RotateCertificatesCommand;
import com.nike.cerberus.command.core.DeleteOldestCertificatesCommand;
import com.nike.cerberus.command.core.RebootCmsCommand;
import com.nike.cerberus.command.core.UpdateStackCommand;
import com.nike.cerberus.command.core.UploadCertificateFilesCommand;
import com.nike.cerberus.command.core.UploadCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.CloudFormationService;

import java.util.List;

/**
 * Operation for the certificate rotation command
 */
public class RotateCertificatesOperation extends CompositeOperation<RotateCertificatesCommand> {

    private final CloudFormationService cloudFormationService;
    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public RotateCertificatesOperation(CloudFormationService cloudFormationService,
                                       EnvironmentMetadata environmentMetadata) {

        this.cloudFormationService = cloudFormationService;
        this.environmentMetadata = environmentMetadata;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain(RotateCertificatesCommand compositeCommand) {
        return Lists.newArrayList(
                // Add the cert and key files to S3
                ChainableCommand.Builder.create().withCommand(new UploadCertificateFilesCommand())
                        .withAdditionalArg(UploadCertificateFilesCommandParametersDelegate.CERT_PATH_LONG_ARG)
                        .withAdditionalArg(compositeCommand.getUploadCertificateFilesCommandParametersDelegate()
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
                ChainableCommand.Builder.create().withCommand(new DeleteOldestCertificatesCommand()).build()
        );
    }

    @Override
    public boolean isRunnable(RotateCertificatesCommand command) {
        boolean isRunnable = true;
        String environmentName = environmentMetadata.getName();

        if (! cloudFormationService.isStackPresent(Stack.LOAD_BALANCER.getFullName(environmentName))) {
            log.error("The load-balancer stack must be present in order to rotate certificates");
            isRunnable = false;
        }

        if (! cloudFormationService.isStackPresent(Stack.CMS.getFullName(environmentName))) {
            log.error("The cms stack must be present to rotate certificates");
            isRunnable = false;
        }

        return isRunnable;
    }
}
