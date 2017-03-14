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
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class S3StoreServiceTest {

    private static final String S3_BUCKET = "fake-bucket";
    private static final String S3_PREFIX = "fake-prefix";

    @Test
    public void testPut() throws IOException {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        String path = "path";
        String value = "value";

        // invoke method under test
        service.put(path, value);

        ArgumentCaptor<String> bucket = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> fullPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> input = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<ObjectMetadata> md = ArgumentCaptor.forClass(ObjectMetadata.class);

        verify(client, times(1)).putObject(bucket.capture(), fullPath.capture(), input.capture(), md.capture());

        assertEquals(S3_BUCKET, bucket.getValue());
        assertEquals(S3_PREFIX + "/" + path, fullPath.getValue());
        assertEquals(value, IOUtils.toString(input.getValue()));
        assertEquals(value.length(), md.getValue().getContentLength());

        verifyNoMoreInteractions(client);
    }

    @Test
    public void testGet() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        String path = "path";
        String value = "value";

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new S3ObjectInputStream(IOUtils.toInputStream(value), mock(HttpRequestBase.class)));

        when(client.getObject(request.capture())).thenReturn(s3Object);

        // invoke method under test
        Optional<String> result = service.get(path);

        assertTrue(result.isPresent());
        assertEquals(value, result.get());

        assertEquals(S3_BUCKET, request.getValue().getBucketName());
        assertEquals(S3_PREFIX + "/" + path, request.getValue().getKey());
    }

    @Test
    public void testGetNoSuchKey() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        String path = "path";
        String value = "value";

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new S3ObjectInputStream(IOUtils.toInputStream(value), mock(HttpRequestBase.class)));

        AmazonServiceException error = new AmazonServiceException("fake expected exception");
        error.setErrorCode("NoSuchKey");

        when(client.getObject(request.capture())).thenThrow(error);

        // invoke method under test
        Optional<String> result = service.get(path);

        assertFalse(result.isPresent());

        assertEquals(S3_BUCKET, request.getValue().getBucketName());
        assertEquals(S3_PREFIX + "/" + path, request.getValue().getKey());
    }

    @Test
    public void testGetMiscAmazonException() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        String path = "path";

        String exMessage = "fake expected exception";
        when(client.getObject(any())).thenThrow(new AmazonServiceException(exMessage));

        try {
            // invoke method under test
            service.get(path);
            fail("expected exception not thrown");
        } catch (AmazonServiceException ex) {
            assertTrue(ex.getMessage().contains(exMessage));
        }
    }


    @Test
    public void testGetIOException() throws IOException {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        InputStream is = mock(InputStream.class);
        when(is.read()).thenThrow(new IOException("fake exception"));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new S3ObjectInputStream(is, mock(HttpRequestBase.class)));

        when(client.getObject(any())).thenReturn(s3Object);

        try {
            // invoke method under test
            service.get("some-path");
            fail("expected exception not thrown");
        } catch (UnexpectedDataEncodingException ex) {
            assertTrue(ex.getMessage().contains("Unable to read contents of S3 object"));
        }
    }

    @Test
    public void testGetKeysInPartialPath() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, S3_PREFIX);

        String path = "path";
        String key = "my-key";

        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey(S3_PREFIX + key);

        ObjectListing listing = mock(ObjectListing.class);

        when(listing.getObjectSummaries()).thenReturn(Lists.newArrayList(summary));
        when(client.listObjects(S3_BUCKET, S3_PREFIX + "/" + path)).thenReturn(listing);

        // invoke method under test
        Set<String> results = service.getKeysInPartialPath(path);

        assertEquals(1, results.size());
        assertEquals(key, results.iterator().next());
    }

    @Test
    public void testDeleteAllKeysOnPartialPath() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, "");

        String path = "path";

        String key = "my-key";

        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey(key);

        ObjectListing listing = mock(ObjectListing.class);

        when(listing.getObjectSummaries()).thenReturn(Lists.newArrayList(summary));
        when(client.listObjects(S3_BUCKET, path)).thenReturn(listing);

        // invoke method under test
        service.deleteAllKeysOnPartialPath(path);

        ArgumentCaptor<DeleteObjectsRequest> request = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(client, times(1)).deleteObjects(request.capture());

        assertEquals(key, request.getValue().getKeys().get(0).getKey());
    }

    @Test
    public void testDeleteAllKeysOnPartialPathListingEmpty() {
        AmazonS3 client = mock(AmazonS3.class);
        S3StoreService service = new S3StoreService(client, S3_BUCKET, "");

        String path = "path";

        when(client.listObjects(S3_BUCKET, path)).thenReturn(mock(ObjectListing.class));

        // invoke method under test
        service.deleteAllKeysOnPartialPath(path);

        verify(client).listObjects(S3_BUCKET, path);
        verifyNoMoreInteractions(client);
    }
}