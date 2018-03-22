/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.operation.core;

import com.amazonaws.regions.Regions;
import com.nike.cerberus.command.core.SyncConfigCommand;
import com.nike.cerberus.operation.Operation;
import com.nike.cerberus.store.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.nike.cerberus.module.CerberusModule.CONFIG_REGION;

/**
 * Operation for syncing configs between regions.
 */
public class SyncConfigOperation implements Operation<SyncConfigCommand> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigStore configStore;

    private Regions configRegion;

    @Inject
    public SyncConfigOperation(ConfigStore configStore, @Named(CONFIG_REGION) String configRegion) {
        this.configStore = configStore;
        this.configRegion = Regions.fromName(configRegion);
    }

    @Override
    public void run(SyncConfigCommand command) {
        if (!command.isAll() && configRegion.equals(Regions.fromName(command.getDestinationRegionName()))){
            throw new RuntimeException("The source and destination region must be different.");
        }
        List<Regions> destinationRegions = command.isAll() ? configStore.getSyncDestinationRegions() : Arrays.asList(Regions.fromName(command.getDestinationRegionName()));

        if (command.isDryrun()){
            logger.info("Destination buckets: {}", destinationRegions.stream()
                    .map(region -> configStore.getConfigBucketForRegion(region)).collect(Collectors.joining(", ")));
            logger.info("Files to be copied over:");
            configStore.listKeys().forEach(key -> logger.info(key));
        } else {
            for (Regions region: destinationRegions) {
                logger.info("Destination bucket: {}", configStore.getConfigBucketForRegion(region));
                configStore.sync(region);
            }
        }

    }

    @Override
    public boolean isRunnable(SyncConfigCommand command) {
        return true;
    }
}