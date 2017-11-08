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

package com.nike.cerberus.domain.cloudformation;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.StackDelegate.PARAMETER_SHORT_ARG;

/**
 * CloudFormation input parameters common to all Cerberus CloudFormation stacks.
 */
public class TagParametersDelegate {

    public static final String OWNER_EMAIL_LONG_ARG = "--owner-email";
    public static final String OWNER_GROUP_LONG_ARG = "--owner-group";
    public static final String COST_CENTER_LONG_ARG = "--costcenter";

    @Parameter(names = "--tag-name",
            description = "The environment name (e.g. 'cerberus-demo', 'cerberus-preprod')")
    private String tagName;

    @Parameter(names = OWNER_EMAIL_LONG_ARG,
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.",
            required = true)
    private String tagEmail;

    @Parameter(names = COST_CENTER_LONG_ARG,
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.",
            required = true)
    private String tagCostcenter;

    public String getTagName() {
        return tagName;
    }

    public TagParametersDelegate setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    public String getTagEmail() {
        return tagEmail;
    }

    public TagParametersDelegate setTagEmail(String tagEmail) {
        this.tagEmail = tagEmail;
        return this;
    }

    public String getTagCostcenter() {
        return tagCostcenter;
    }

    public TagParametersDelegate setTagCostcenter(String tagCostcenter) {
        this.tagCostcenter = tagCostcenter;
        return this;
    }
}
