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

package com.nike.cerberus.operation.composite;

import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.LinkedList;
import java.util.List;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "We don't care"
)
/**
 * Wrapper object to represent a chainable command.
 *
 * @See CompositeOperation
 */
public class ChainableCommand {

    private Command command;
    private String[] additionalArgs = new String[]{};


    public ChainableCommand() {
    }

    public ChainableCommand(Command command) {
        this.command = command;
    }

    public ChainableCommand(Command command, String[] additionalArgs) {
        this.command = command;
        this.additionalArgs = additionalArgs;
    }

    public Command getCommand() {
        return command;
    }

    public String[] getAdditionalArgs() {
        return additionalArgs;
    }

    public static final class Builder {
        private Command command;
        private List<String> additionalArgs = new LinkedList<>();

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withCommand(Command command) {
            this.command = command;
            return this;
        }

        public Builder withAdditionalArg(String additionalArg) {
            additionalArgs.add(additionalArg);
            return this;
        }

        public Builder withOption(String key, String value) {
            additionalArgs.add(key);
            additionalArgs.add(value);
            return this;
        }

        public ChainableCommand build() {
            ChainableCommand chainableCommand = new ChainableCommand();
            chainableCommand.command = this.command;
            chainableCommand.additionalArgs = this.additionalArgs.toArray(new String[additionalArgs.size()]);
            return chainableCommand;
        }
    }
}
