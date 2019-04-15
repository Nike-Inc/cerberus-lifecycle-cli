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

package com.nike.cerberus.command.composite;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.composite.CreateCmsResourcesForRegionOperation;

import static com.nike.cerberus.command.composite.CreateEnvironmentCommand.COMMAND_NAME;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION_DESCRIPTION;

@Parameters(
    commandNames = {
        COMMAND_NAME
    },
    commandDescription = "Stands up the resources needed to have CMS running in a given region"
)
public class CreateCmsResourcesForRegionCommand implements Command {

  public static final String COMMAND_NAME = "create-resources-for-secondary-region";

  @Parameter(names = STACK_REGION, description = STACK_REGION_DESCRIPTION, required = true)
  private String stackRegion;

  public String getStackRegion() {
    return stackRegion;
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  public Class<? extends Operation<?>> getOperationClass() {
    return CreateCmsResourcesForRegionOperation.class;
  }
}
