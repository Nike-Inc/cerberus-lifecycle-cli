/*
 * Copyright (c) 2021 Nike, Inc.
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
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateInstanceProfileOperation;

import java.util.Optional;

import static com.nike.cerberus.command.core.CreateInstanceProfileCommand.COMMAND_NAME;

@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the instance profile used for Cerberus.")
public class CreateInstanceProfileCommand implements Command {

    public static final String COMMAND_NAME = "create-instance-profile";

    public static final String INSTANCE_PROFILE_IAM_ROLE_LONG_ARG = "--iam-role";

    @ParametersDelegate
    private CloudFormationParametersDelegate cloudFormationParametersDelegate = new CloudFormationParametersDelegate();

    public CloudFormationParametersDelegate getCloudFormationParametersDelegate() {
        return cloudFormationParametersDelegate;
    }

    @Parameter(
        names = {
                INSTANCE_PROFILE_IAM_ROLE_LONG_ARG
        },
        description = "The iam role name that should be used to create the instance profile that will get applied to the EC2 instances, "
    )
    private String instanceProfileIamRole;

    public Optional<String> getInstanceProfileIamRole() {
        return instanceProfileIamRole == null ? Optional.empty() : Optional.of(instanceProfileIamRole);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateInstanceProfileOperation.class;
    }
}
