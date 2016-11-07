/*
 * Copyright (c) 2016 Nike Inc.
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

package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.LambdaName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.PublishLambdaOperation;

import java.net.URL;

import static com.nike.cerberus.command.gateway.PublishLambdaCommand.COMMAND_NAME;

/**
 * Command for uploading a new lambda.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Publishes the lambda artifact to Cerberus.")
public class PublishLambdaCommand implements Command {

    public static final String COMMAND_NAME = "publish-lambda";

    @Parameter(names = "--lambda-name", description = "Which lambda is being uploaded.", required = true)
    private LambdaName lambdaName;

    @Parameter(names = "--artifact-url", description = "URL to the lambda artifact.", required = true)
    private URL artifactUrl;

    public LambdaName getLambdaName() {
        return lambdaName;
    }

    public URL getArtifactUrl() {
        return artifactUrl;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return PublishLambdaOperation.class;
    }
}
