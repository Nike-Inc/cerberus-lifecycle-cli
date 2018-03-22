/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.command.composite;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.cloudformation.TagParametersDelegate;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.composite.UpdateAllStackTagsOperation;

import static com.nike.cerberus.command.composite.UpdateAllStackTagsCommand.COMMAND_NAME;


/**
 * Command for updating all CloudFormation stacks with tags.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates all CloudFormation stacks with tags.")
public class UpdateAllStackTagsCommand implements Command {
    public static final String COMMAND_NAME = "update-all-stack-tags";

    @ParametersDelegate
    private TagParametersDelegate tagsDelegate = new TagParametersDelegate();

    public TagParametersDelegate getTagsDelegate() {
        return tagsDelegate;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateAllStackTagsOperation.class;
    }
}
