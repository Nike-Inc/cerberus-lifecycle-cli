/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.command;

import com.beust.jcommander.Parameter;

/**
 * Represents CloudFormation stack parameters that are common to all Cerberus cluster components.
 */
public class StackDelegate {

    @Parameter(names = "--ami-id", description = "The AMI ID for the specified stack.", required = true)
    private String amiId;

    @Parameter(names = "--instance-size", description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = "--key-pair-name", required = true, description = "SSH key pair name.")
    private String keyPairName;

    @Parameter(names = "--owner-group",
            description = "The owning group for the provision resources. Will be tagged on all resources.",
            required = true)
    private String ownerGroup;

    @Parameter(names = "--owner-email",
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.",
            required = true)
    private String ownerEmail;

    @Parameter(names = "--costcenter",
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.",
            required = true)
    private String costcenter;

    @Parameter(names = "--desired-instances", description = "Desired number of auto scaling instances.")
    private int desiredInstances = 3;

    @Parameter(names = "--max-instances", description = "Maximum number of auto scaling instances.")
    private int maximumInstances = 3;

    @Parameter(names = "--min-instances", description = "Minimum number of auto scaling instances")
    private int minimumInstances = 3;

    public String getAmiId() {
        return amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCostcenter() {
        return costcenter;
    }

    public int getDesiredInstances() {
        return desiredInstances;
    }

    public int getMaximumInstances() {
        return maximumInstances;
    }

    public int getMinimumInstances() {
        return minimumInstances;
    }
}
