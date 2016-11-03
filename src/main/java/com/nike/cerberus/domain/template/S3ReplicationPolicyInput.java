package com.nike.cerberus.domain.template;

/**
 * S3 replication policy configuration input.
 */
public class S3ReplicationPolicyInput {

    private String sourceBucket;

    private String replicationBucket;

    public String getSourceBucket() {
        return sourceBucket;
    }

    public S3ReplicationPolicyInput setSourceBucket(String sourceBucket) {
        this.sourceBucket = sourceBucket;
        return this;
    }

    public String getReplicationBucket() {
        return replicationBucket;
    }

    public S3ReplicationPolicyInput setReplicationBucket(String replicationBucket) {
        this.replicationBucket = replicationBucket;
        return this;
    }
}
