/*
 * Copyright (c) 2016 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
