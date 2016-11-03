package com.nike.cerberus.domain.environment;

/**
 * Describes the lambdas that are part of Cerberus.
 */
public enum LambdaName {
    WAF("waf", "lambda/waf.jar"),
    CLOUD_FRONT_SG_GROUP_IP_SYNC("cf-sg-ip-sync", "lambda/cf-sg-ip-sync.zip");

    private final String name;

    private final String bucketKey;

    LambdaName(final String name, final String bucketKey) {
        this.name = name;
        this.bucketKey = bucketKey;
    }

    public String getName() {
        return name;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public static LambdaName fromName(final String name) {
        for (LambdaName lambdaName : LambdaName.values()) {
            if (lambdaName.getName().equals(name)) {
                return lambdaName;
            }
        }

        throw new IllegalArgumentException("Unknown lambda name: " + name);
    }
}
