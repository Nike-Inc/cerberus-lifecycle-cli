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

package com.nike.cerberus.operation.dashboard;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;

import java.io.File;

/**
 * Metadata provider for dashboard assets
 *
 * We use this to set some headers we want s3 to send with our assets
 */
public class DashboardMetaDataProvider implements ObjectMetadataProvider {
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String CONTENT_TYPE = "Content-Type";

    @Override
    public void provideObjectMetadata(File file, ObjectMetadata metadata) {
        metadata.setHeader(CACHE_CONTROL, "private, no-cache, no-store, proxy-revalidate, no-transform");
        if (file.getName().endsWith(".html")) {
            metadata.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");
        }
    }
}
