package com.nike.cerberus.command;

import com.beust.jcommander.Parameter;

/**
 * Represents CloudFormation stack parameters that are common to all Cerberus cluster components.
 */
public class StackDelegate {

    @Parameter(names = "--ami-id", description = "The AMI ID for the specified stack.", required = true)
    private String amiId;

    @Parameter(names = "--instance-size", description = "Specify a custom instance size.")
    private String instanceSize;

    @Parameter(names = "--key-pair-name", required = true, description = "SSH key pair name.")
    private String keyPairName;

    @Parameter(names = "--owner-group",
            description = "The owning group for the provision resources. Will be tagged on all resources.",
            required = true)
    private String ownerGroup;

    @Parameter(names = "--owner-email",
            description = "The e-mail for who owns the provisioned resources. Will be tagged on all resources.",
            required = true)
    private String ownerEmail;

    @Parameter(names = "--costcenter",
            description = "Costcenter for where to bill provisioned resources. Will be tagged on all resources.",
            required = true)
    private String costcenter;

    public String getAmiId() {
        return amiId;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCostcenter() {
        return costcenter;
    }
}
