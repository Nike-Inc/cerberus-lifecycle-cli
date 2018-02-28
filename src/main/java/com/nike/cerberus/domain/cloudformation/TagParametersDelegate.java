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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CloudFormation input parameters common to all Cerberus CloudFormation stacks.
 */
public class TagParametersDelegate {

    public static final String TAG_LONG_ARG = "--TAG";
    public static final String TAG_SHORT_ARG = "-T";

    @DynamicParameter(
            names = {
                    TAG_LONG_ARG,
                    TAG_SHORT_ARG
            },
            description = "Dynamic parameters for setting tags to be used on generated aws resources."
    )
    private Map<String, String> tags = new HashMap<>();

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return tags.entrySet().stream().map(e->String.format("--TAG%s=%s",e.getKey(),e.getValue())).collect(Collectors.joining(" "));
    }
}
