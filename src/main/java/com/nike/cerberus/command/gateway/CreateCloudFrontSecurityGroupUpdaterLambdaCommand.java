/*
 * Copyright (c) 2016 Nike Inc.
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

package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.CreateCloudFrontSecurityGroupUpdaterLambdaOperation;

import static com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand.COMMAND_NAME;

/**
 * This command creates the Lambda needed to update the Security Groups that limit traffic ingress to only IPs coming
 * from AWS Cloud Front.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "creates the Lambda needed to update the Security Groups that limit traffic ingress to only IPs coming from AWS Cloud Front.")
public class CreateCloudFrontSecurityGroupUpdaterLambdaCommand implements Command {

    public static final String COMMAND_NAME = "create-cloud-front-security-group-updater-lambda";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCloudFrontSecurityGroupUpdaterLambdaOperation.class;
    }
}
