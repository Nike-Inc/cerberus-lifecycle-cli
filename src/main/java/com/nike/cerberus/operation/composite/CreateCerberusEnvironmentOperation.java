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

import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsCmkCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.composite.CreateCerberusEnvironmentCommand;
import com.nike.cerberus.command.core.CreateBaseCommand;
import com.nike.cerberus.command.core.CreateDatabaseCommand;
import com.nike.cerberus.command.core.CreateEdgeDomainRecordCommand;
import com.nike.cerberus.command.core.CreateLoadBalancerCommand;
import com.nike.cerberus.command.core.CreateRoute53Command;
import com.nike.cerberus.command.core.CreateSecurityGroupsCommand;
import com.nike.cerberus.command.core.CreateVpcCommand;
import com.nike.cerberus.command.core.CreateWafCommand;
import com.nike.cerberus.command.core.GenerateCertsCommand;
import com.nike.cerberus.command.core.UploadCertFilesCommand;
import com.nike.cerberus.domain.environment.Stack;

import java.util.List;

/**
 * Operation class for CreateCerberusEnvironmentCommand
 */
public class CreateCerberusEnvironmentOperation extends CompositeOperation<CreateCerberusEnvironmentCommand> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ChainableCommand> getCompositeCommandChain() {
        return ChainableCommandListBuilder.create()

                // Step 1 Create the Base Cloud Formation Stack that creates S3 Buckets, Iam Roles and KMS keys needed for config
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateBaseCommand()).build())

                // Step 2 Create the VPC Cloud Formation Stack that Cerberus will use
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateVpcCommand()).build())

                // Step 3 Create the Security Group Cloud Formation Stack
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateSecurityGroupsCommand()).build())

                // Step 4 Create the RDS Database Cloud Formation Stack
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateDatabaseCommand()).build())

                // Step 5 Generate the PKCS private and public keys as well as the x509 certificates needed to enable https
                .addCommand(ChainableCommand.Builder.create().withCommand(new GenerateCertsCommand()).build())

                // Step 6 Upload the certs and keys to S3 and the IAM Cert Management service so that the ALB and CMS can use the certs
                .addCommand(ChainableCommand.Builder.create().withCommand(new UploadCertFilesCommand())
                        .withAdditionalArg(UploadCertFilesCommand.STACK_NAME_LONG_ARG)
                        .withAdditionalArg(Stack.CMS.getName())
                        .build())

                // Step 7 Create the Application Load Balancer Cloud Formation Stack
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateLoadBalancerCommand()).build())

                // Step 8 Generate the CMS config with org specific setting and first secrets encrypt,
                // Upload to S3 for CMS to download at service start
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateCmsConfigCommand()).build())

                // Step 9 Create CMS CMK, create KMS master keys in the regions specified for CMS to use with the AWS Encryption
                // client to encrypt secure data in a manor that is decryptable in multiple regions and store in cms props
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateCmsCmkCommand()).build())

                // Step 10 Create the CMS Cluster Stack
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateCmsClusterCommand()).build())

                // Step 11 Create the Web Application Fire wall stack
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateWafCommand()).build())

                // Step 12 Create the Route 53 DNS Record Stack for origin and the load balancer
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateRoute53Command()).build())

                // Step 13 Create the outer most domain name record that will point to the origin record
                .addCommand(ChainableCommand.Builder.create().withCommand(new CreateEdgeDomainRecordCommand()).build())

                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunnable(CreateCerberusEnvironmentCommand command) {
        // Always return true and depend on isRunnable of the chained commands to skip commands that cannot be re-ran
        // we want this command to be able to be run more than once to complete an environment for example say temporary
        // aws creds expire half way through environment creation
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean skipOnNotRunnable() {
        // Since we always return true for isRunnable on this command sub of the sub commands may have already been run
        // we want this command to be able to be run more than once to complete an environment for example say temporary
        // aws creds expire half way through environment creation
        return true;
    }
}
