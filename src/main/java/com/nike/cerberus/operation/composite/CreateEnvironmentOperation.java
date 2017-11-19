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
import com.nike.cerberus.command.cms.CreateCmsClusterCommand;
import com.nike.cerberus.command.cms.CreateCmsCmkCommand;
import com.nike.cerberus.command.cms.CreateCmsConfigCommand;
import com.nike.cerberus.command.composite.CreateEnvironmentCommand;
import com.nike.cerberus.command.core.*;

import java.util.List;

/**
 * Operation class for CreateCerberusEnvironmentCommand
 */
public class CreateEnvironmentOperation extends CompositeOperation<CreateEnvironmentCommand> {

    /**
     * {@inheritDoc}
     * @param compositeCommand
     */
    @Override
    protected List<ChainableCommand> getCompositeCommandChain(CreateEnvironmentCommand compositeCommand) {
        List<ChainableCommand> list = Lists.newArrayList(
            // Step 1 Create the Base Cloud Formation Stack that creates S3 Buckets, Iam Roles and KMS keys needed for config
            new ChainableCommand(new CreateBaseCommand()),

            // Step 2 Create the VPC Cloud Formation Stack that Cerberus will use
            new ChainableCommand(new CreateVpcCommand()),

            // Step 3 Create the Security Group Cloud Formation Stack
            new ChainableCommand(new CreateSecurityGroupsCommand()),

            // Step 3.5 Add the vpc whitelist CIDRs
            new ChainableCommand(new WhitelistCidrForVpcAccessCommand()),

            // Step 4 Create the RDS Database Cloud Formation Stack
            new ChainableCommand(new CreateDatabaseCommand())
        );

        // Step 5 Generate the PKCS private and public keys as well as the x509 certificates needed to enable https
        if (environmentConfig.isGenerateKeysAndCerts()) {
            list.add(new ChainableCommand(new GenerateCertificateFilesCommand()));
        }
                
        list.addAll(Lists.newArrayList(
            // Step 6 Upload the certs and keys to S3 and the IAM Cert Management service so that the ALB and CMS can use the certs
            new ChainableCommand(new UploadCertificateFilesCommand()),

            // Step 7 Create the Application Load Balancer Cloud Formation Stack
            new ChainableCommand(new CreateLoadBalancerCommand()),

            // Step 8 Generate the CMS config with org specific setting and first secrets encrypt,
            // Upload to S3 for CMS to download at service start
            new ChainableCommand(new CreateCmsConfigCommand()),

            // Step 9 Create CMS CMK, create KMS master keys in the regions specified for CMS to use with the AWS Encryption
            // client to encrypt secure data in a manor that is decryptable in multiple regions and store in cms props
            new ChainableCommand(new CreateCmsCmkCommand()),

            // Step 10 Create the CMS Cluster Stack
            new ChainableCommand(new CreateCmsClusterCommand()),

            // Step 11 Create the Web Application Fire wall stack
            new ChainableCommand(new CreateWafCommand()),

            // Step 12 Create the Route 53 DNS Record Stack for origin and the load balancer
            new ChainableCommand(new CreateRoute53Command()),

            // Step 13 Create the outer most domain name record that will point to the origin record
            new ChainableCommand(new CreateEdgeDomainRecordCommand())
        ));

        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunnable(CreateEnvironmentCommand command) {
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
