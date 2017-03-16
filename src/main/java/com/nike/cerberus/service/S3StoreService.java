/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.nike.cerberus.ConfigConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service wrapper for AWS S3.
 */
public class S3StoreService implements StoreService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonS3 s3Client;

    private final String s3Bucket;

    private final String s3Prefix;

    public S3StoreService(final AmazonS3 s3Client, final String s3Bucket, final String s3Prefix) {
        this.s3Client = s3Client;
        this.s3Bucket = s3Bucket;
        this.s3Prefix = s3Prefix;
    }

    public void put(String path, String value) {
        byte[] content;
        try {
            content = value.getBytes(ConfigConstants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new UnexpectedDataEncodingException("Value to be stored has unexpected encoding.", e);
        }
        ByteArrayInputStream contentAsStream = new ByteArrayInputStream(content);
        ObjectMetadata md = new ObjectMetadata();
        md.setContentLength(content.length);
        s3Client.putObject(s3Bucket, getFullPath(path), contentAsStream, md);
    }


    public Optional<String> get(String path) {
        Optional<S3Object> s3ObjectOptional = getS3Object(path);
        if (! s3ObjectOptional.isPresent()) {
            return Optional.empty();
        }

        try {
            InputStream object = s3ObjectOptional.get().getObjectContent();
            return Optional.of(IOUtils.toString(object, ConfigConstants.DEFAULT_ENCODING));
        } catch (IOException e) {
            String errorMessage =
                    String.format("Unable to read contents of S3 object. Bucket: %s, Key: %s, Expected Encoding: %s",
                            s3Bucket, getFullPath(path), ConfigConstants.DEFAULT_ENCODING);
            logger.error(errorMessage);
            throw new UnexpectedDataEncodingException(errorMessage, e);
        }
    }

    protected Optional<S3Object> getS3Object(String path) {
        GetObjectRequest request = new GetObjectRequest(s3Bucket, getFullPath(path));
        try {
            return Optional.of(s3Client.getObject(request));
        }
        catch (AmazonServiceException ase) {
            if (StringUtils.equalsIgnoreCase(ase.getErrorCode(), "NoSuchKey")) {
                logger.debug(String.format("The S3 object doesn't exist. Bucket: %s, Key: %s", s3Bucket, request.getKey()));
                return Optional.empty();
            } else {
                logger.error("Unexpected error communicating with AWS.", ase);
                throw ase;
            }
        }
    }

    public Map<String, String> getS3ObjectUserMetaData(String path) {
        Optional<S3Object> encryptedObjectOptional = getS3Object(path);
        if (! encryptedObjectOptional.isPresent()) {
            throw new RuntimeException("Cannot get metadata for an S3 Object that is not present");
        }
        S3Object s3Object = encryptedObjectOptional.get();
        return s3Object.getObjectMetadata().getUserMetadata();
    }

    public Set<String> getKeysInPartialPath(String path) {
        ObjectListing objectListing = s3Client.listObjects(s3Bucket, getFullPath(path));

        return objectListing
                        .getObjectSummaries()
                        .stream()
                        .map(objectSummary -> StringUtils.stripStart(objectSummary.getKey(), s3Prefix + "/"))
                        .collect(Collectors.toSet());
    }

    public void deleteAllKeysOnPartialPath(String path) {
        ObjectListing objectListing = s3Client.listObjects(s3Bucket, getFullPath(path));

        if (objectListing.getObjectSummaries().isEmpty()) {
            return;
        }

        List<DeleteObjectsRequest.KeyVersion> keys = objectListing
                .getObjectSummaries()
                .stream()
                .map(objectSummary -> new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()))
                .collect(Collectors.toList());

        DeleteObjectsRequest request = new DeleteObjectsRequest(s3Bucket);
        request.setKeys(keys);
        s3Client.deleteObjects(request);
    }

    private String getFullPath(final String path) {
        if (StringUtils.isBlank(s3Prefix)) {
            return path;
        } else {
            return String.format("%s/%s", s3Prefix, path);
        }
    }
}
