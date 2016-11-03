package com.nike.cerberus.command.core;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.core.WhitelistCidrForVpcAccessOpertaion;

import java.util.ArrayList;
import java.util.List;

import static com.nike.cerberus.command.core.WhitelistCidrForVpcAccessCommand.COMMAND_NAME;

/**
 * Command for granting CIDRs ingress to specific ports within the Cerberus VPC.
 */
@Parameters(commandNames = COMMAND_NAME, commandDescription = "Creates the base components to support Cerberus.")
public class WhitelistCidrForVpcAccessCommand implements Command {

    public static final String COMMAND_NAME = "whitelist-cidr-for-vpc-access";

    @Parameter(names = "-cidr", description = "One or more CIDRs to be granted ingress on the Cerberus VPC.")
    private List<String> cidrs = new ArrayList<>();

    @Parameter(names = "-port", description = "The ports to grant ingress on within the Cerberus VPC.")
    private List<Integer> ports = new ArrayList<>();

    public List<String> getCidrs() {
        return cidrs;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return WhitelistCidrForVpcAccessOpertaion.class;
    }
}
