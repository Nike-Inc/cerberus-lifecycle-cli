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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.SyncConfigOperation;

import static com.nike.cerberus.command.core.UpdateStackCommand.COMMAND_NAME;


/**
 * Command for syncing configs between regions.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Syncs configs between regions. Recursively copies all files from the source region's config bucket (as defined in --region) to the destination region's config bucket.")
public class SyncConfigCommand implements Command {

    public static final String COMMAND_NAME = "sync-config";
    public static final String DESTINATION_REGION_LONG_ARG = "--destination-region";
    public static final String DRY_LONG_ARG = "--dry";
    public static final String ALL_LONG_ARG = "--all";

    @Parameter(names = {DESTINATION_REGION_LONG_ARG}, description = "The destination region")
    private String destinationRegionName;

    @Parameter(names = {DRY_LONG_ARG}, description = "Displays destination buckets and files to be copied over without actually running them")
    private boolean dryrun;

    @Parameter(names = {ALL_LONG_ARG}, description = "Sync up all regions as defines in the environment data")
    private boolean all;

    public String getDestinationRegionName() {
        return destinationRegionName;
    }

    public boolean isDryrun() {
        return dryrun;
    }

    public boolean isAll() {
        return all;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return SyncConfigOperation.class;
    }
}
