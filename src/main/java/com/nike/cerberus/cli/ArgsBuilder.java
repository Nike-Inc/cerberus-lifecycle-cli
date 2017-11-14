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

    public List<String> build() {
        return args;
    }
}