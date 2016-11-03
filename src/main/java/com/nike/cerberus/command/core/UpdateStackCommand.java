package com.nike.cerberus.command.core;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.domain.environment.StackName;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.UpdateStackOperation;

import java.util.HashMap;
import java.util.Map;

import static com.nike.cerberus.command.core.UpdateStackCommand.COMMAND_NAME;

/**
 * Command for updating the specified CloudFormation stack with the new parameters.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the specified CloudFormation stack.")
public class UpdateStackCommand implements Command {

    public static final String COMMAND_NAME = "update-stack";

    @Parameter(names = {"--stack-name"}, required = true, description = "The stack name to update.")
    private StackName stackName;

    @Parameter(names = "--owner-group",
            description = "The owning group for the resources to be updated. Will be tagged on all resources.",
            required = true)
    private String ownerGroup;

    @Parameter(names = "--ami-id", description = "The AMI ID for the specified stack.")
    private String amiId;

    @Parameter(names = "--instance-size", description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = "--key-pair-name", description = "SSH key pair name.")
    private String keyPairName;

    @Parameter(names = "--owner-email",
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.")
    private String ownerEmail;

    @Parameter(names = "--costcenter",
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.")
    private String costcenter;

    @Parameter(names = "--overwrite-template",
            description = "Flag for overwriting existing CloudFormation template")
    private boolean overwriteTemplate;

    @DynamicParameter(names = "-P", description = "Dynamic parameters for overriding the values for specific parameters in the CloudFormation.")
    private Map<String, String> dynamicParameters = new HashMap<>();

    public StackName getStackName() {
        return stackName;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public String getAmiId() {
        return amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCostcenter() {
        return costcenter;
    }

    public boolean isOverwriteTemplate() {
        return overwriteTemplate;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateStackOperation.class;
    }
}
