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

package com.nike.cerberus.cli;

import java.util.LinkedList;
import java.util.List;

public class ArgsBuilder {

    private List<String> args;

    private ArgsBuilder() {
        args = new LinkedList<>();
    }

    public static ArgsBuilder create() {
        return new ArgsBuilder();
    }

    public ArgsBuilder addOption(String optionKey, String optionValue) {
        args.add(optionKey);
        args.add(optionValue);
        return this;
    }

    public ArgsBuilder addDynamicProperty(String dynamicOptionFlag, String key, String value) {
        args.add(String.format("%s%s=%s", dynamicOptionFlag, key, value));
        return this;
    }

    public ArgsBuilder addFlag(String flag) {
        args.add(flag);
        return this;
    }

    public ArgsBuilder addAll(List<String> argsToAdd) {
        this.args.addAll(argsToAdd);
        return this;
    }

    /**
     * Adds an argument option to the args list using the supplied value unless passed args contains the option and a value.
     * @param optionKey The option to add to the args
     * @param optionValue The option value to use if passed args doesn't already contain the option key and value
     * @param passedArgs The passed args from the user
     *
     * @return The builder
     */
    public ArgsBuilder addOptionUsingPassedArgIfPresent(String optionKey, String optionValue, String [] passedArgs) {
        int index = -1;
        for (int i = 0; i < passedArgs.length; i++) {
            if (optionKey.equals(passedArgs[i])) {
                index = i;
                break;
            }
        }

        args.add(optionKey);
        if (index > -1 && index < passedArgs.length - 2) {
            args.add(passedArgs[index + 1]);
        } else {
            args.add(optionValue);
        }

        return this;
    }

    public List<String> build() {
        return args;
    }
}