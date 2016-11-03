package com.nike.cerberus.domain.cloudformation;

public class CloudFrontIpSynchronizerOutputs {

    private String cloudFrontOriginElbSgIpSyncFunctionArn;

    public String getCloudFrontOriginElbSgIpSyncFunctionArn() {
        return cloudFrontOriginElbSgIpSyncFunctionArn;
    }

    public CloudFrontIpSynchronizerOutputs setCloudFrontOriginElbSgIpSyncFunctionArn(String cloudFrontOriginElbSgIpSyncFunctionArn) {
        this.cloudFrontOriginElbSgIpSyncFunctionArn = cloudFrontOriginElbSgIpSyncFunctionArn;
        return this;
    }
}
