package com.nike.cerberus.command.core;

import com.amazonaws.regions.Regions;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.CreateAlbLogAthenaDbAndTableOperation;

import java.util.Optional;

import static com.nike.cerberus.command.audit.CreateAuditAthenaDbAndTableCommand.COMMAND_NAME;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION;
import static com.nike.cerberus.domain.cloudformation.CloudFormationParametersDelegate.STACK_REGION_DESCRIPTION;

@Parameters(
        commandNames = COMMAND_NAME,
        commandDescription = "Creates the db and table needed in athena to enable interacting with the ALB logs via athena"
)
public class CreateAlbLogAthenaDbAndTableCommand implements Command {

    public static final String COMMAND_NAME = "create-alb-log-athena-db-and-table";

    @Parameter(names = STACK_REGION, description = STACK_REGION_DESCRIPTION)
    private String stackRegion;

    public Optional<Regions> getStackRegion() {
        return stackRegion == null ? Optional.empty() : Optional.of(Regions.fromName(stackRegion));
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return CreateAlbLogAthenaDbAndTableOperation.class;
    }
}
