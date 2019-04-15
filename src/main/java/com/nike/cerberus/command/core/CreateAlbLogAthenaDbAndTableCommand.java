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

import com.amazonaws.regions.Regions;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateAlbLogAthenaDbAndTableOperation;

import java.util.Optional;

import static com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand.COMMAND_NAME;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION_DESCRIPTION;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Creates the db and table needed in athena to enable interacting with the ALB logs via athena"
)
public class CreateAlbLogAthenaDbAndTableCommand implements Command {

    public static final String COMMAND_NAME = "create-alb-log-athena-db-and-table";

    @Parameter(names = STACK_REGION, description = STACK_REGION_DESCRIPTION)
    private String stackRegion;

    public Optional<Regions> getStackRegion() {
        return stackRegion == null ? Optional.empty() : Optional.of(Regions.fromName(stackRegion));
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateAlbLogAthenaDbAndTableOperation.class;
    }
}
