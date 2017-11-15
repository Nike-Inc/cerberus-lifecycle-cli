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

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Versions;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.nike.cerberus.command.core.DeleteStackCommand;
import com.nike.cerberus.domain.EnvironmentMetadata;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.UnexpectedCloudFormationStatusException;
import com.nike.cerberus.service.CloudFormationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteStackOperation implements Operation<DeleteStackCommand> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EnvironmentMetadata environmentMetadata;

    private final CloudFormationService cloudFormationService;

    private final AmazonS3 amazonS3;

    @Inject
    public DeleteStackOperation(EnvironmentMetadata environmentMetadata,
                                CloudFormationService cloudFormationService, AmazonS3 amazonS3) {

        this.environmentMetadata = environmentMetadata;
        this.cloudFormationService = cloudFormationService;
        this.amazonS3 = amazonS3;
    }

    @Override
    public void run(DeleteStackCommand command) {
        String stackId = command.getStack().getFullName(environmentMetadata.getName());
        cloudFormationService.getStackResources(stackId)
                .forEach(stackResourceSummary -> {
                    if (stackResourceSummary.getResourceType().equals("AWS::S3::Bucket")) {
                        String bucketName = stackResourceSummary.getPhysicalResourceId();
                        for (S3VersionSummary version : S3Versions.inBucket(amazonS3, bucketName)) {
                            String key = version.getKey();
                            String versionId = version.getVersionId();
                            amazonS3.deleteVersion(bucketName, key, versionId);
                        }
                    }
                });

        cloudFormationService.deleteStackAndWait(stackId);
    }

    @Override
    public boolean isRunnable(DeleteStackCommand command) {
        if (! cloudFormationService.isStackPresent(command.getStack().getFullName(environmentMetadata.getName()))) {
            log.error("The stack: {} does not exists", command.getStack());
            return false;
        }

        return true;
    }
}