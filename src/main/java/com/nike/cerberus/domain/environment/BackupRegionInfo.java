/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.domain.environment;

public class BackupRegionInfo {
    private String s3Bucket;
    private String kmsCmkId;

    public BackupRegionInfo() {
    }

    public BackupRegionInfo(String bucket, String kmsCmkId) {
        this.s3Bucket = bucket;
        this.kmsCmkId = kmsCmkId;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getKmsCmkId() {
        return kmsCmkId;
    }

    public void setKmsCmkId(String kmsCmkId) {
        this.kmsCmkId = kmsCmkId;
    }
}
