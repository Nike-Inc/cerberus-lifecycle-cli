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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.RestoreCerberusBackupOperation;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

/**
 * Command for restoring Safe Deposit Box Metadata and Vault secret data for SDBs from backups that are in S3 from
 * the cross region backup lambda.
 */
@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Allows Cerberus operators to restore a complete backup from S3 that was created using the the backup command."
)
public class RestoreCerberusBackupCommand implements Command {

    public static final String COMMAND_NAME = "restore-complete";

    @Parameter(names = "-s3-region",
            description = "The region for the bucket that contains the backups",
            required = true
    )
    private String s3Region;

    @Parameter(names = "-s3-bucket",
            description = "The bucket that contains the backups",
            required = true
    )
    private String s3Bucket;

    @Parameter(names = "-s3-prefix",
            description = "the folder that contains the json backup files",
            required = true
    )
    private String s3Prefix;

    @Parameter(names = "-url",
            description = "The cerberus api, to restore to",
            required = true
    )
    private String cerberusUrl;

    public String getS3Region() {
        return s3Region;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3Prefix() {
        return s3Prefix;
    }

    public String getCerberusUrl() {
        return cerberusUrl;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return RestoreCerberusBackupOperation.class;
    }
}
