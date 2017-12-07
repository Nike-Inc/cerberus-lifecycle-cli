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

import com.amazonaws.regions.Regions;
import com.google.common.collect.Maps;
import com.nike.cerberus.ConfigConstants;
import com.nike.cerberus.domain.environment.Stack;
import com.nike.cerberus.store.ConfigStore;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.module.CerberusModule.ENV_NAME;

/**
 * Service for generating EC2 user data for Cerberus instances.
 */
public class Ec2UserDataService {

    private final String environmentName;
    private final ConfigStore configStore;

    @Inject
    public Ec2UserDataService(@Named(ENV_NAME) String environmentName,
                              ConfigStore configStore) {

        this.environmentName = environmentName;
        this.configStore = configStore;
    }

    public String getUserData(Regions region, Stack stack, Optional<String> group) {
        if (stack.equals(Stack.CMS)) {
            return getCmsUserData(region, group);
        } else {
            throw new IllegalArgumentException("The stack specified does not support user data. stack: "
                    + stack.getName());
        }
    }

    private String getCmsUserData(Regions region, Optional<String> group) {
        Map<String, String> userDataMap = Maps.newHashMap();
        addStandardEnvironmentVariables(region, userDataMap, Stack.CMS.getName(), group);

        return encodeUserData(writeExportEnvVars(userDataMap));
    }

    private void addStandardEnvironmentVariables(Regions region,
                                                 Map<String, String> userDataMap,
                                                 String appName,
                                                 Optional<String> group) {

        userDataMap.put("CLOUD_ENVIRONMENT", ConfigConstants.ENV_PREFIX + environmentName);
        userDataMap.put("CLOUD_MONITOR_BUCKET", appName);
        userDataMap.put("CLOUD_APP", appName);
        userDataMap.put("CLOUD_APP_GROUP", group.orElse("cerberus"));
        userDataMap.put("CLOUD_CLUSTER", appName);
        userDataMap.put("CLASSIFICATION", "Gold");
        userDataMap.put("EC2_REGION", region.getName());
        userDataMap.put("AWS_REGION", region.getName());
        userDataMap.put("CONFIG_S3_BUCKET", configStore.getConfigBucketForRegion(region));
        userDataMap.put("CONFIG_KEY_ID", configStore.getEnvironmentDataSecureDataKmsCmkRegion(region));
    }

    private String writeExportEnvVars(Map<String, String> userDataMap) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : userDataMap.entrySet()) {
            stringBuilder.append(String.format("export %s=\"%s\"%n", entry.getKey(), entry.getValue()));
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String encodeUserData(String userData) {
        return Base64.getEncoder().encodeToString(userData.getBytes(Charset.forName("UTF-8")));
    }
}
