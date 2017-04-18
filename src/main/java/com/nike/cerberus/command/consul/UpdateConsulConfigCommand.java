package com.nike.cerberus.command.consul;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.nike.cerberus.command.Command;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.operation.consul.UpdateConsulConfigOperation;

import static com.nike.cerberus.command.consul.CreateConsulConfigCommand.COMMAND_NAME;

@Parameters(commandNames = COMMAND_NAME, commandDescription = "Updates the consul configuration for the cluster while maintaining the secrets in the current config.")
public class UpdateConsulConfigCommand implements Command {

    public static final String COMMAND_NAME = "update-consul-config";

    @Parameter(names = {"--acl-master-token"}, description = "Overwrites the existing ACL Master Token with the value supplied."
            + " If not supplied, the existing token in secrets.json will be maintained (safer and generally preferred).")
    private String aclMasterToken = null;

    @Parameter(names = {"--gossip-encryption-token"}, description = "Overwrites the existing Gossip Encryption Token with the value supplied."
            + " If not supplied, the existing token in secrets.json will be maintained (safer and generally preferred).")
    private String gossipEncryptionToken = null;

    public String getAclMasterToken() {
        return aclMasterToken;
    }

    public String getGossipEncryptionToken() {
        return gossipEncryptionToken;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Class<? extends Operation<?>> getOperationClass() {
        return UpdateConsulConfigOperation.class;
    }
}