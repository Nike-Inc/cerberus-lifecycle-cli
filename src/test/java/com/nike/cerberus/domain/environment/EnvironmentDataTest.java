/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.module.CerberusModule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EnvironmentDataTest {

    @Test
    public void test_that_environment_data_can_be_serialized_and_deserialized() throws IOException {
        EnvironmentData expected = new EnvironmentData();
        expected.setEnvironmentName("test-env-name");
        expected.setAdminIamRoleArn("admin-role");
        expected.setCmsIamRoleArn("cms-role");
        expected.setRootIamRoleArn("root-role");
        RegionData regionData = new RegionData();
        regionData.setPrimary(true);
        regionData.setConfigBucket("foo");
        regionData.setEnvironmentDataSecureDataKmsCmkArn("bar");
        regionData.setCmsSecureDataKmsCmkArn("bam");
        expected.addRegionData(Regions.US_WEST_2, regionData);

        ObjectMapper mapper = CerberusModule.configObjectMapper();

        String serializedEnvData = mapper.writeValueAsString(expected);

        EnvironmentData actual = mapper.readValue(serializedEnvData, EnvironmentData.class);

        assertEquals(expected.getEnvironmentName(), actual.getEnvironmentName());
        assertEquals(expected.getAdminIamRoleArn(), actual.getAdminIamRoleArn());
        assertEquals(expected.getCmsIamRoleArn(), actual.getCmsIamRoleArn());
        assertEquals(expected.getRootIamRoleArn(), actual.getRootIamRoleArn());
        assertEquals(expected.getRegionData().size(), actual.getRegionData().size());
        assertEquals(expected.getPrimaryRegion(), actual.getPrimaryRegion());
    }

}
