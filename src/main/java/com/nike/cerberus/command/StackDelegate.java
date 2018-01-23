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

package com.nike.cerberus.command;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents CloudFormation stack parameters that are common to all Cerberus cluster components.
 */
public class StackDelegate {

    public static final String AMI_ID_LONG_ARG = "--ami-id";
    public static final String INSTANCE_SIZE_LONG_ARG = "--instance-size";
    public static final String KEY_PAIR_NAME_LONG_ARG = "--key-pair-name";
    public static final String PARAMETER_SHORT_ARG = "-P";

    @Parameter(names = AMI_ID_LONG_ARG, description = "The AMI ID for the specified stack.", required = true)
    private String amiId;

    @Parameter(names = INSTANCE_SIZE_LONG_ARG, description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = KEY_PAIR_NAME_LONG_ARG, required = true, description = "SSH key pair name.")
    private String keyPairName;

    @ParametersDelegate
    private TagParametersDelegate tagParameters = new TagParametersDelegate();

    @DynamicParameter(names = PARAMETER_SHORT_ARG, description = "Dynamic parameters for overriding the values for specific parameters in the CloudFormation.")
    private Map<String, String> dynamicParameters = new HashMap<>();

    public String getAmiId() {
        return amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public TagParametersDelegate getTagParameters() {
        return tagParameters;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }
}
