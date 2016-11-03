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
