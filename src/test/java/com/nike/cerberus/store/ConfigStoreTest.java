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

package com.nike.cerberus.store;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.domain.environment.EnvironmentData;
import com.nike.cerberus.domain.environment.RegionData;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.service.EncryptionService;
import com.nike.cerberus.service.StoreService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigStoreTest {
    @Mock
    private EncryptionService encryptionService;

    @Spy
    private StoreService storeServiceUswest2;

    @Spy
    private StoreService storeServiceUseast1;

    @Spy
    private StoreService storeServiceUseast2;

    private ObjectMapper objectMapper;
    private ConfigStore configStore;
    private EnvironmentData initEnvironmentdata = new EnvironmentData();
    private List<StoreService> storeServices;

    @Before
    public void setUp() {
        initMocks(this);
        storeServices = Arrays.asList(storeServiceUseast1, storeServiceUseast2, storeServiceUswest2);
        initEnvironmentdata.addRegionData(Regions.US_WEST_2, new RegionData());
        initEnvironmentdata.addRegionData(Regions.US_EAST_1, new RegionData());
        initEnvironmentdata.addRegionData(Regions.US_EAST_2, new RegionData());
        objectMapper = CerberusModule.configObjectMapper();

        configStore = spy(new ConfigStore(null, null, null,
                null, objectMapper, null, "env1",
                "us-west-2", encryptionService));

        doReturn(initEnvironmentdata).when(configStore).getDecryptedEnvironmentData();
        doReturn(storeServiceUswest2).when(configStore).getStoreServiceForRegion(Regions.US_WEST_2, initEnvironmentdata);
        doReturn(storeServiceUseast1).when(configStore).getStoreServiceForRegion(Regions.US_EAST_1, initEnvironmentdata);
        doReturn(storeServiceUseast2).when(configStore).getStoreServiceForRegion(Regions.US_EAST_2, initEnvironmentdata);
    }

    @Test
    public void testIdenticalConfig() {
        storeServices.forEach(s -> doReturn(ImmutableSet.of("a.txt", "cms/b.txt")).when(s).getKeysInPartialPath(any()));
        storeServices.forEach(s -> doReturn(Optional.of("a.txt hash")).when(s).getHash(argThat(h -> h.equals("a.txt"))));
        storeServices.forEach(s -> doReturn(Optional.of("b.txt hash")).when(s).getHash(argThat(h -> h.equals("cms/b.txt"))));

        assertTrue(configStore.isConfigSynchronized());
    }

    @Test
    public void testMissingFile() {
        doReturn(ImmutableSet.of("a.txt", "cms/b.txt")).when(storeServiceUswest2).getKeysInPartialPath(any());
        doReturn(ImmutableSet.of("a.txt", "cms/b.txt")).when(storeServiceUseast1).getKeysInPartialPath(any());
        doReturn(ImmutableSet.of("a.txt")).when(storeServiceUseast2).getKeysInPartialPath(any());

        storeServices.forEach(s -> doReturn(Optional.of("a.txt hash")).when(s).getHash(argThat(h -> h.equals("a.txt"))));
        storeServices.forEach(s -> doReturn(Optional.of("b.txt hash")).when(s).getHash(argThat(h -> h.equals("cms/b.txt"))));

        assertFalse(configStore.isConfigSynchronized());
    }

    @Test
    public void testMismatchedHash() {
        storeServices.forEach(s -> doReturn(ImmutableSet.of("a.txt", "cms/b.txt")).when(s).getKeysInPartialPath(any()));
        storeServices.forEach(s -> doReturn(Optional.of("a.txt hash")).when(s).getHash(argThat(h -> h.equals("a.txt"))));
        doReturn(Optional.of("b.txt hash")).when(storeServiceUswest2).getHash(argThat(h -> h.equals("cms/b.txt")));
        doReturn(Optional.of("b.txt hash v2")).when(storeServiceUseast1).getHash(argThat(h -> h.equals("cms/b.txt")));
        doReturn(Optional.of("b.txt hash v2")).when(storeServiceUseast2).getHash(argThat(h -> h.equals("cms/b.txt")));

        assertFalse(configStore.isConfigSynchronized());
    }
}
