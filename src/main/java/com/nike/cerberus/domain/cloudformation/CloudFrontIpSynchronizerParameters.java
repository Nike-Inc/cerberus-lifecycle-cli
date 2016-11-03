package com.nike.cerberus.domain.cloudformation;

public class CloudFrontIpSynchronizerParameters {

    private String lambdaBucket;

    private String lambdaKey;

    public String getLambdaBucket() {
        return lambdaBucket;
    }

    public CloudFrontIpSynchronizerParameters setLambdaBucket(String lambdaBucket) {
        this.lambdaBucket = lambdaBucket;
        return this;
    }

    public String getLambdaKey() {
        return lambdaKey;
    }

    public CloudFrontIpSynchronizerParameters setLambdaKey(String lambdaKey) {
        this.lambdaKey = lambdaKey;
        return this;
    }
}
