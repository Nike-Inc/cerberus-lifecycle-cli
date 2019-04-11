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

package com.nike.cerberus.command;

import com.beust.jcommander.JCommander;
import org.junit.Test;

import java.net.Proxy;

import static org.junit.Assert.assertEquals;

public class CerberusCommandTest {

    @Test
    public void test_that_command_line_poxy_args_are_honored() {
        String[] userInput = {
                "--proxy-type", "SOCKS",
                "--proxy-host", "localhost",
                "--proxy-port", "9000",
                "COMMAND",
                "--some-opt", "some-value"
        };

        CerberusCommand cerberusCommand = new CerberusCommand();
        JCommander commander = new JCommander(cerberusCommand);
        commander.setProgramName("cerberus");
        commander.setAcceptUnknownOptions(true);
        commander.parseWithoutValidation(userInput);

        ProxyDelegate proxyDelegate = cerberusCommand.getProxyDelegate();

        assertEquals("localhost", proxyDelegate.getProxyHost());
        assertEquals((Integer) 9000, proxyDelegate.getProxyPort());
        assertEquals(Proxy.Type.SOCKS, proxyDelegate.getProxyType());
    }

}
