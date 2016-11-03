package com.nike.cerberus.domain;

import com.amazonaws.regions.Regions;

/**
 * Environment metadata.  Everything but the bucket name is immutable.  Bucket name isn't known immediately
 * if running operation is provisioning it!
 */
public class EnvironmentMetadata {

    private final String name;

    private final String regionName;

    private String bucketName;

    public EnvironmentMetadata(final String name, final String regionName) {
        this.name = name;
        this.regionName = regionName;
    }

    public String getName() {
        return name;
    }

    public String getRegionName() {
        return regionName;
    }

    public Regions getRegions() {
        return Regions.fromName(regionName);
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
