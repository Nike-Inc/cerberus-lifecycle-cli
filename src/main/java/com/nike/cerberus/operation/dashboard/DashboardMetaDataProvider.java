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
