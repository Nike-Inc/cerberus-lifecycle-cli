package com.nike.cerberus.command.gateway;

import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.gateway.CreateCloudFrontSecurityGroupUpdaterLambdaOperation;

import static com.nike.cerberus.command.gateway.CreateCloudFrontLogProcessingLambdaConfigCommand.COMMAND_NAME;

/**
 * This command creates the Lambda needed to update the Security Groups that limit traffic ingress to only IPs coming
 * from AWS Cloud Front.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "creates the Lambda needed to update the Security Groups that limit traffic ingress to only IPs coming from AWS Cloud Front.")
public class CreateCloudFrontSecurityGroupUpdaterLambdaCommand implements Command {

    public static final String COMMAND_NAME = "create-cloud-front-security-group-updater-lambda";

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateCloudFrontSecurityGroupUpdaterLambdaOperation.class;
    }
}
