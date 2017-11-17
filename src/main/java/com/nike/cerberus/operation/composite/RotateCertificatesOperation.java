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
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.core.AddCertificateToAlbCommand;
import com.nike.cerberus.command.composite.RotateCertificatesCommand;
import com.nike.cerberus.command.core.GenerateCertificateFilesCommand;
import com.nike.cerberus.command.core.RollingRebootWithHealthCheckCommand;
import com.nike.cerberus.command.core.UploadCertificateFilesCommand;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.service.CloudFormationService;

import java.util.LinkedList;
import java.util.List;

public class RotateCertificatesOperation extends CompositeOperation<RotateCertificatesCommand> {

    private final CloudFormationService cloudFormationService;

    @Inject
    public RotateCertificatesOperation(CloudFormationService cloudFormationService) {
        this.cloudFormationService = cloudFormationService;
    }

    @Override
    protected List<ChainableCommand> getCompositeCommandChain() {
        List<ChainableCommand> commandList = new LinkedList<>();

        if (environmentConfig.isGenerateKeysAndCerts()) {
            commandList.add(ChainableCommand.Builder.create().withCommand(new GenerateCertificateFilesCommand()).build());
        }

        commandList.addAll(Lists.newArrayList(
                // Add the cert and key files to S3
                ChainableCommand.Builder.create().withCommand(new UploadCertificateFilesCommand()).build(),
                // Add the new cert to the ALB
                ChainableCommand.Builder.create().withCommand(new AddCertificateToAlbCommand()).build(),
                // Generate new CMS config that points to the new cert
                ChainableCommand.Builder.create().withCommand(new CreateCmsConfigCommand()).build(),
                // Do a rolling reboot of the management service
                ChainableCommand.Builder.create().withCommand(new RollingRebootWithHealthCheckCommand()).build()
                // Delete the old cert from the ALB and from the Identity Management service and S3

                // Revoke the Cert? todo
        ));

        return commandList;
    }

    @Override
    public boolean isRunnable(RotateCertificatesCommand command) {
        boolean isRunnable = true;

        if (! cloudFormationService.isStackPresent(Stack.LOAD_BALANCER.getName())) {
            log.error("The load-balancer stack must be present in order to rotate certificates");
            isRunnable = false;
        }

        if (! cloudFormationService.isStackPresent(Stack.CMS.getName())) {
            log.error("The cms stack must be present to rotate certificates");
        }

        return isRunnable;
    }
}
