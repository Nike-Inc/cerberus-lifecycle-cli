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
import com.nike.cerberus.command.composite.GenerateAndRotateCertificatesCommand;
import com.nike.cerberus.command.composite.RotateCertificatesCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommandParametersDelegate;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.CloudFormationService;

import javax.inject.Inject;
import java.util.List;

import static com.nike.cerberus.command.core.UploadCertificateFilesCommandParametersDelegate.CERT_PATH_LONG_ARG;
import static com.nike.cerberus.command.core.GenerateCertificateFilesCommandParametersDelegate.*;

public class GenerateAndRotateCertificatesOperation extends CompositeOperation<GenerateAndRotateCertificatesCommand> {

    private final CloudFormationService cloudFormationService;
    private final EnvironmentMetadata environmentMetadata;

    @Inject
    public GenerateAndRotateCertificatesOperation(CloudFormationService cloudFormationService,
                                                  EnvironmentMetadata environmentMetadata) {

        this.cloudFormationService = cloudFormationService;
        this.environmentMetadata = environmentMetadata;
    }


    @Override
    protected List<ChainableCommand> getCompositeCommandChain(GenerateAndRotateCertificatesCommand compositeCommand) {
        GenerateCertificateFilesCommandParametersDelegate parameters = compositeCommand
                .getGenerateCertificateFilesCommandParametersDelegate();

        ChainableCommand.Builder generateCertificateFilesCommandBuilder = ChainableCommand.Builder.create()
                .withCommand(new GenerateCertificateFilesCommand())
                .withOption(
                        BASE_DOMAIN_LONG_ARG,
                        parameters.getBaseDomainName()
                )
                .withOption(
                        EDGE_DOMAIN_NAME_OVERRIDE_LONG_ARG,
                        parameters.getEdgeDomainNameOverride()
                )
                .withOption(
                        ORIGIN_DOMAIN_NAME_OVERRIDE_LONG_ARG,
                        parameters.getOriginDomainNameOverride()
                )
                .withOption(
                        LOAD_BALANCER_DOMAIN_NAME_OVERRIDE_LONG_ARG,
                        parameters.getLoadBalancerDomainNameOverride()
                )
                .withOption(
                        HOSTED_ZONE_ID_LONG_ARG,
                        parameters.getHostedZoneId()
                )
                .withOption(
                        ENABLE_LE_CERTFIX_LONG_ARG,
                        String.valueOf(parameters.enableLetsEncryptCertfix())
                )
                .withOption(
                        CERT_FOLDER_LONG_ARG,
                        parameters.getCertDir()
                )
                .withOption(
                        ACME_API_LONG_ARG,
                        parameters.getAcmeApiUrl()
                )
                .withOption(
                        CONTACT_EMAIL_LONG_ARG,
                        parameters.getContactEmail()
                );

                parameters.getSubjectAlternativeNames().forEach(name -> {
                    generateCertificateFilesCommandBuilder.withOption(SUBJECT_ALT_NAME_LONG_ARG, name);
                });

        return Lists.newArrayList(
                // Generate the Certificates
                generateCertificateFilesCommandBuilder.build(),

                // Rotate the certificate files
                ChainableCommand.Builder.create().withCommand(new RotateCertificatesCommand())
                        .withOption(
                                CERT_PATH_LONG_ARG,
                                parameters.getCertDir())
                        .build()
        );
    }

    @Override
    public boolean isRunnable(GenerateAndRotateCertificatesCommand command) {
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
