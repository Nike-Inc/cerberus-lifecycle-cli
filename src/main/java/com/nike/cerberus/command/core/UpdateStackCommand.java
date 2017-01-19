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

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.UpdateStackOperation;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.core.UpdateStackCommand.COMMAND_NAME;

/**
 * Command for updating the specified CloudFormation stack with the new parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the specified CloudFormation stack.")
public class UpdateStackCommand implements Command {

    public static final String COMMAND_NAME = "update-stack";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to update.")
    private StackName stackName;

    @Parameter(names = "--owner-group",
            description = "The owning group for the resources to be updated. Will be tagged on all resources.",
            required = true)
    private String ownerGroup;

    @Parameter(names = "--ami-id", description = "The AMI ID for the specified stack.")
    private String amiId;

    @Parameter(names = "--instance-size", description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = "--key-pair-name", description = "SSH key pair name.")
    private String keyPairName;

    @Parameter(names = "--owner-email",
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.")
    private String ownerEmail;

    @Parameter(names = "--costcenter",
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.")
    private String costcenter;

    @Parameter(names = "--overwrite-template",
            description = "Flag for overwriting existing CloudFormation template")
    private boolean overwriteTemplate;

    @Parameter(names = "--desired-instances", description = "Desired number of auto scaling instances.")
    private Integer desiredInstances;

    @Parameter(names = "--max-instances", description = "Maximum number of auto scaling instances.")
    private Integer maximumInstances;

    @Parameter(names = "--min-instances", description = "Minimum number of autos scaling instances")
    private Integer minimumInstances;

    @DynamicParameter(names = "-P", description = "Dynamic parameters for overriding the values for specific parameters in the CloudFormation.")
    private Map<String, String> dynamicParameters = new HashMap<>();

    public StackName getStackName() {
        return stackName;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public String getAmiId() {
        return amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCostcenter() {
        return costcenter;
    }

    public boolean isOverwriteTemplate() {
        return overwriteTemplate;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }

    public Integer getDesiredInstances() {
        return desiredInstances;
    }

    public Integer getMaximumInstances() {
        return maximumInstances;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateStackOperation.class;
    }
}
