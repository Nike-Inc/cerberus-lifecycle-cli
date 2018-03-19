package com.nike.cerberus.store;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.environment.EnvironmentData;
import com.nike.cerberus.domain.environment.RegionData;
import com.nike.cerberus.module.CerberusModule;
import com.nike.cerberus.service.EncryptionService;
import com.nike.cerberus.service.StoreService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigStoreTest {
    @Mock
    EncryptionService encryptionService;

    @Spy
    StoreService storeServiceUswest2;

    @Spy
    StoreService storeServiceUseast1;

    @Spy
    StoreService storeServiceUseast2;

    private ObjectMapper objectMapper;
    private ConfigStore configStore;

    private final String environmentProperties1 = "ramen_1:yuzu\nramen_2:kizuki\n";
    private final String environmentProperties2 = "ramen_1:ryoma\nramen_2:kizuki\n";
    private EnvironmentData initEnvironmentdata = new EnvironmentData();

    @Before
    public void setUp() {
        initEnvironmentdata.addRegionData(Regions.US_WEST_2, new RegionData());
        initEnvironmentdata.addRegionData(Regions.US_EAST_1, new RegionData());
        initEnvironmentdata.addRegionData(Regions.US_EAST_2, new RegionData());
        objectMapper = CerberusModule.configObjectMapper();
        initMocks(this);
        configStore = spy(new ConfigStore(null, null, null,
                null, objectMapper, null, "env1",
                "us-west-2", encryptionService));

        doReturn(initEnvironmentdata).when(configStore).getDecryptedEnvironmentData();
        doReturn(storeServiceUswest2).when(configStore).getStoreServiceForRegion(Regions.US_WEST_2, initEnvironmentdata);
        doReturn(storeServiceUseast1).when(configStore).getStoreServiceForRegion(Regions.US_EAST_1, initEnvironmentdata);
        doReturn(storeServiceUseast2).when(configStore).getStoreServiceForRegion(Regions.US_EAST_2, initEnvironmentdata);

        doReturn(environmentProperties1).when(encryptionService).decrypt(argThat(s -> s.equals("i'm encrypted properties")));
        doReturn(environmentProperties2).when(encryptionService).decrypt(argThat(s -> s.equals("i'm encrypted properties too")));



    }

    @Test
    public void testIdenticalConfig() {
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUswest2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUseast1).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUseast2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUswest2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUseast1).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUseast2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        assertTrue(configStore.isConfigSynchronized());
    }

    @Test
    public void testMismatchedEnvironmentData() {
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUswest2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUseast1).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted too")).when(storeServiceUseast2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUswest2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUseast1).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUseast2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        assertFalse(configStore.isConfigSynchronized());
    }

    @Test
    public void testMismatchedProperties() {
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUswest2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUseast1).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted")).when(storeServiceUseast2).get(ConfigConstants.ENVIRONMENT_DATA_FILE);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUswest2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties")).when(storeServiceUseast1).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        doReturn(Optional.of("i'm encrypted properties too")).when(storeServiceUseast2).get(ConfigConstants.CMS_ENV_CONFIG_PATH);
        assertFalse(configStore.isConfigSynchronized());
    }
}