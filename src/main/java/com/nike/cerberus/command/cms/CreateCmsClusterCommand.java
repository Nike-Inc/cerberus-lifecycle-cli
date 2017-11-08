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

package com.nike.cerberus.command.cms;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.command.StackDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.CreateCmsClusterOperation;

import static com.nike.cerberus.command.cms.CreateCmsClusterCommand.COMMAND_NAME;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_ARG;
import static com.nike.cerberus.ConfigConstants.SKIP_AMI_TAG_CHECK_DESCRIPTION;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the CMS cluster.")
public class CreateCmsClusterCommand implements Command {

    public static final String COMMAND_NAME = "create-cms-cluster";

    @ParametersDelegate
    private StackDelegate stackDelegate = new StackDelegate();

    public StackDelegate getStackDelegate() {
        return stackDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Parameter(names = SKIP_AMI_TAG_CHECK_ARG,
            description = SKIP_AMI_TAG_CHECK_DESCRIPTION)
    private boolean skipAmiTagCheck;

    public boolean isSkipAmiTagCheck() {
        return skipAmiTagCheck;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsClusterOperation.class;
    }
}