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

package com.nike.cerberus.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.EnableKeyRotationRequest;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.beust.jcommander.internal.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Convenience methods for calls to KMS
 */
@Singleton
public class KmsService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private KmsClientFactory kmsClientFactory;

    @Inject
    public KmsService(KmsClientFactory kmsClientFactory) {
        this.kmsClientFactory = kmsClientFactory;
    }

    public List<String> createKeysAndAliases(List<Regions> regions, String alias, String policy, String description) {
        List<String> keyArns = Lists.newArrayList();
        for (Regions region : regions) {
            String keyArn = createKey(region, policy, description, alias);
            keyArns.add(keyArn);
        }
        return keyArns;
    }

    /**
     * Create a CMK in the supplied region with the supplied policy and description
     *
     * @param region      the target region
     * @param policy      the default key poicy
     * @param description the Key description
     * @param alias       the CMK alias to generate for this key
     * @return cmkArn
     */
    private String createKey(Regions region, String policy, String description, String alias) {

        AWSKMS client = kmsClientFactory.getClient(region);

        CreateKeyRequest request = new CreateKeyRequest();
        request.setPolicy(policy);
        request.setDescription(description);
        CreateKeyResult result = client.createKey(request);
        KeyMetadata keyMetadata = result.getKeyMetadata();

        String keyArn = keyMetadata.getArn();
        String keyId = keyMetadata.getKeyId();

        logger.info(String.format("Created CMK '%s' in region '%s'", keyArn, region));

        enableKeyRotation(region, keyId);
        createAlias(region, keyId, alias);

        return keyArn;
    }


    /**
     * Ensure CMK key rotation is enabled for the supplied key
     */
    private void enableKeyRotation(Regions region, String keyId) {
        AWSKMS client = kmsClientFactory.getClient(region);

        client.enableKeyRotation(new EnableKeyRotationRequest().withKeyId(keyId));
        logger.info(String.format("Enabled key rotation for '%s' in '%s'", keyId, region));

    }

    /**
     * Create a CMK Alias for the supplied region and keyId
     */
    private void createAlias(Regions region, String keyId, String alias) {
        AWSKMS client = kmsClientFactory.getClient(region);

        CreateAliasRequest request = new CreateAliasRequest();
        request.setAliasName(alias);
        request.setTargetKeyId(keyId);
        client.createAlias(request);

        logger.info(String.format("Created Alias '%s' for key '%s' in region '%s'", alias, keyId, region));
    }

}
