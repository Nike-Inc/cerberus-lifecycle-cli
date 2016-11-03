package com.nike.cerberus.command.dashboard;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.dashboard.PublishDashboardOperation;

import java.net.URL;

import static com.nike.cerberus.command.dashboard.PublishDashboardCommand.COMMAND_NAME;

/**
 * Command for uploading a new dashboard.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Publishes the dashboard artifact to Cerberus.")
public class PublishDashboardCommand implements Command {

    public static final String COMMAND_NAME = "publish-dashboard";

    @Parameter(names = "--artifact-url", description = "URL to the dashboard artifact.", required = true)
    private URL artifactUrl;

    public URL getArtifactUrl() {
        return artifactUrl;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return PublishDashboardOperation.class;
    }
}
