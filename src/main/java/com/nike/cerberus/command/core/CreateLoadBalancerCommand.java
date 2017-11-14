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

package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateLoadBalancerOperation;

import static com.nike.cerberus.command.core.CreateLoadBalancerCommand.COMMAND_NAME;

/**
 * Command to create the load balancer for Cerberus.
 */
@Parameters(commandNames = COMMAND_NAME,
        commandDescription = "Create the load balancer used for Cerberus.")
public class CreateLoadBalancerCommand implements Command {

    public static final String COMMAND_NAME = "create-load-balancer";

    public static final String LOAD_BALANCER_SSL_POLICY_OVERRIDE_LONG_ARG = "--ssl-policy-override";

    @ParametersDelegate
    private TagParametersDelegate tagsDelegate = new TagParametersDelegate();

    public TagParametersDelegate getTagsDelegate() {
        return tagsDelegate;
    }

    @Parameter(
            names = {
                    LOAD_BALANCER_SSL_POLICY_OVERRIDE_LONG_ARG
            },
            description = "The SSL Policy that will get applied to the application load balancer, " +
                    "see http://docs.aws.amazon.com/elasticloadbalancing/latest/classic/elb-security-policy-table.html" +
                    " for more information."
    )
    private String loadBalancerSslPolicyOverride;

    public String getLoadBalancerSslPolicyOverride() {
        return loadBalancerSslPolicyOverride;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateLoadBalancerOperation.class;
    }

}
