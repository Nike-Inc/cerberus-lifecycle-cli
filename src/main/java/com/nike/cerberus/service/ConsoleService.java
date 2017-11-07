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

package com.nike.cerberus.service;

import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * To allow us to run this code base in IDE's and have access to civilized dev tools we will have this util
 * that will handle System.console returning null in dev envs, as a result in Dev modes secrets will be entered
 * in plain text on the console
 */
@Singleton
public class ConsoleService {

    public String readLine(String format, Object... args) throws IOException {
        if (System.console() != null) {
            return System.console().readLine(format, args);
        }
        System.out.print(String.format(format, args));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        return reader.readLine();
    }

    public char[] readPassword(String format, Object... args) throws IOException {
        if (System.console() != null) {
            return System.console().readPassword(format, args);
        }
        return readLine(format, args).toCharArray();
    }

    public void askUserToProceed(String additionalMessage, DefaultAction defaultAction) {
        String userInput;
        try {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(additionalMessage)) {
                sb.append(additionalMessage).append('\n');
            }
            sb.append("Would you like to proceed? ")
                    .append(defaultAction.isYesDefault() ? "(Y/n)" : "(y/N)")
                    .append(": ");
            userInput = readLine(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to ask user to proceed", e);
        }

        if (StringUtils.isBlank(userInput) && defaultAction.isYesDefault()) {
            return;
        }

        if (StringUtils.isNotBlank(userInput) && (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes"))) {
            return;
        }

        throw new RuntimeException("User declined to proceed");
    }

    public enum DefaultAction {
        YES(true),
        NO(false);

        private boolean yesIsDefault;

        protected boolean isYesDefault() {
            return yesIsDefault;
        }

        DefaultAction(boolean yesIsDefault) {
            this.yesIsDefault = yesIsDefault;
        }
    }
}
