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

package com.nike.cerberus.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import com.github.tomaslanger.chalk.Chalk;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;

/**
 * Class for printing CLI help and usage.
 */
public class CerberusHelp {

    private JCommander commander;

    public CerberusHelp(JCommander commander) {
        this.commander = commander;
    }

    public void print() {
        String commandName = commander.getParsedCommand();
        if (StringUtils.isNotBlank(commandName)) {
            commander.usage(commandName);
        } else {
            printCustomUsage();
        }
    }

    /**
     * Usage with nicer formatting than we get out of JCommander by default
     */
    private void printCustomUsage() {
        StringBuilder sb = new StringBuilder("Usage: cerberus [options] [command] [command options]\n");

        String indent = "";
        //indenting
        int descriptionIndent = 6;
        int indentCount = indent.length() + descriptionIndent;

        int longestName = 0;
        List<ParameterDescription> sorted = Lists.newArrayList();
        for (ParameterDescription pd : commander.getParameters()) {
            if (!pd.getParameter().hidden()) {
                sorted.add(pd);
                // + to have an extra space between the name and the description
                int length = pd.getNames().length() + 2;
                if (length > longestName) {
                    longestName = length;
                }
            }
        }

        sb.append(indent).append("  Options:\n");

        sorted.stream()
                .sorted((p0, p1) -> p0.getLongestName().compareTo(p1.getLongestName()))
                .forEach(pd -> {
                    WrappedParameter parameter = pd.getParameter();
                    sb.append(indent).append("  "
                            + (parameter.required() ? "* " : "  ")
                            + Chalk.on(pd.getNames()).green().bold().toString()
                            + "\n");
                    wrapDescription(sb, indentCount, s(indentCount) + pd.getDescription());
                    Object def = pd.getDefault();
                    if (def != null) {
                        String displayedDef = StringUtils.isBlank(def.toString())
                                ? "<empty string>"
                                : def.toString();
                        sb.append("\n" + s(indentCount)).append("Default: " + Chalk.on(displayedDef).yellow().bold().toString());
                    }
                    Class<?> type = pd.getParameterized().getType();
                    if (type.isEnum()) {
                        String values = EnumSet.allOf((Class<? extends Enum>) type).toString();
                        sb.append("\n" + s(indentCount)).append("Possible Values: " + Chalk.on(values).yellow().bold().toString());
                    }
                    sb.append("\n");
                });

        System.out.println(sb.toString());
        System.out.print("  ");
        printCommands();
    }

    private void printCommands() {
        System.out.println("Commands, use cerberus [-h, --help] [command name] for more info:");
        commander.getCommands().keySet().stream().sorted().forEach(command -> {
            String msg = String.format("    %s, %s",
                    Chalk.on(command).green().bold().toString(),
                    commander.getCommandDescription(command));
            System.out.println(msg);
        });
    }

    private String s(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(" ");
        }

        return result.toString();
    }

    private void wrapDescription(StringBuilder out, int indent, String description) {
        int max = 79;
        String[] words = description.split(" ");
        int current = 0;
        int i = 0;
        while (i < words.length) {
            String word = words[i];
            if (word.length() > max || current + 1 + word.length() <= max) {
                out.append(word).append(" ");
                current += word.length() + 1;
            } else {
                out.append("\n").append(s(indent)).append(word).append(" ");
                current = indent + 1 + word.length();
            }
            i++;
        }
    }
}
