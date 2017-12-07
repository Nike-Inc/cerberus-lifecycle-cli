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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Versions;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.google.inject.Inject;
import com.nike.cerberus.command.core.DeleteStackCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.service.AwsClientFactory;
import com.nike.cerberus.service.CloudFormationService;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

public class DeleteStackOperation implements Operation<DeleteStackCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String environmentName;

    private final CloudFormationService cloudFormationService;

    private final AwsClientFactory<AmazonS3Client> amazonS3Factory;

    private final ConfigStore configStore;

    @Inject
    public DeleteStackOperation(@Named(ENV_NAME) String environmentName,
                                CloudFormationService cloudFormationService,
                                AwsClientFactory<AmazonS3Client> amazonS3Factory,
                                ConfigStore configStore) {

        this.environmentName = environmentName;
        this.cloudFormationService = cloudFormationService;
        this.amazonS3Factory = amazonS3Factory;
        this.configStore = configStore;
    }

    @Override
    public void run(DeleteStackCommand command) {
        Regions region = getRegion(command);

        String stackName = command.getStack().getFullName(environmentName);
        cloudFormationService.getStackResources(region, stackName)
                .forEach(stackResourceSummary -> {
                    if (stackResourceSummary.getResourceType().equals("AWS::S3::Bucket")) {
                        String bucketName = stackResourceSummary.getPhysicalResourceId();
                        log.info("Detected S3 Bucket: {}, emptying contents before deleting stack", bucketName);
                        AmazonS3 amazonS3 = amazonS3Factory.getClient(region);
                        for (S3VersionSummary version : S3Versions.inBucket(amazonS3, bucketName)) {
                            String key = version.getKey();
                            String versionId = version.getVersionId();
                            amazonS3.deleteVersion(bucketName, key, versionId);
                        }
                    }
                });

        log.info("Deleting stack: {} in region: {}", stackName, region);
        cloudFormationService.deleteStackAndWait(region, stackName);
        log.info("Finished deleting stack: {} in regionL {}", stackName, region);
    }

    private Regions getRegion(DeleteStackCommand command) {
        return command.getRegion() == null ? configStore.getPrimaryRegion() : Regions.fromName(command.getRegion());
    }

    @Override
    public boolean isRunnable(DeleteStackCommand command) {
        Regions region = getRegion(command);
        if (! cloudFormationService.isStackPresent(region, command.getStack().getFullName(environmentName))) {
            log.error("The stack: {} does not exist in region: {}", command.getStack(), region.getName());
            return false;
        }

        return true;
    }
}