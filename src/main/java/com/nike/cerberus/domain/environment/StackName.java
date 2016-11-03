package com.nike.cerberus.domain.environment;

/**
 * Describes the stacks that make up Cerberus.
 */
public enum StackName {
    BASE("base"),
    CONSUL("consul"),
    VAULT("vault"),
    CMS("cms"),
    GATEWAY("gateway"),
    @Deprecated
    LAMBDA("lambda"),
    RDSBACKUP("rdsbackup"),
    CLOUD_FRONT_IP_SYNCHRONIZER("cloud-front-ip-synchronizer");

    private final String name;

    StackName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static StackName fromName(final String name) {
        for (StackName stackName : StackName.values()) {
            if (stackName.getName().equals(name)) {
                return stackName;
            }
        }

        throw new IllegalArgumentException("Unknown stack name: " + name);
    }
}
