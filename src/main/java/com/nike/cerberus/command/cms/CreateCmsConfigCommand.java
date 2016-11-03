package com.nike.cerberus.command.cms;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.cms.CreateCmsConfigOperation;

import static com.nike.cerberus.command.cms.CreateCmsClusterCommand.COMMAND_NAME;

/**
 * Command to create the CMS cluster.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the CMS config.")
public class CreateCmsConfigCommand implements Command {

    public static final String COMMAND_NAME = "create-cms-config";

    @Parameter(names = "--admin-group", description = "Group that has admin privileges in CMS.", required = true)
    private String adminGroup;


    public String getAdminGroup() {
        return adminGroup;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCmsConfigOperation.class;
    }
}
