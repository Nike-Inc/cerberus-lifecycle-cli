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

package com.nike.cerberus.service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
import com.nike.cerberus.util.CiphertextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class EncryptionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AwsCrypto awsCrypto;

    @Inject
    public EncryptionService(AwsCrypto awsCrypto) {
        this.awsCrypto = awsCrypto;
    }

    /**
     * Encrypt the plainTextPayload.
     * <p>
     * Generates a Base64 encoded String the the 'AWS Encryption SDK Message Format'
     * <p>
     * http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
     *
     * @param plainTextPayload the secrets to encrypt
     */
    public String encrypt(MasterKeyProvider<KmsMasterKey> encryptProvider, String plainTextPayload) {
        return awsCrypto.encryptString(encryptProvider, plainTextPayload).getResult();
    }

    /**
     * Decrypt the encryptedPayload.
     * <p>
     * Expects a Base64 encoded String the the 'AWS Encryption SDK Message Format'.
     * <p>
     * http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
     *
     * @param encryptedPayload encryptedPayload
     */
    public String decrypt(String encryptedPayload) {
        ParsedCiphertext parsedCiphertext = CiphertextUtils.parse(encryptedPayload);
        // Parses the ARNs out of the encryptedPayload so that you can manually rotate the CMKs, if desired
        // Whatever CMKs were used in the encrypt operation will be used to decrypt
        try {
            List<String> cmkArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
            MasterKeyProvider<KmsMasterKey> decryptProvider = initializeKeyProvider(cmkArns);
            return new String(awsCrypto.decryptData(decryptProvider, parsedCiphertext).getResult(), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            log.error("Decrypt operation failed " + CiphertextUtils.toJson(parsedCiphertext), e);
            throw e;
        }
    }

    /**
     * Initialize a Multi-KMS-MasterKeyProvider.
     * <p>
     * For encrypt, KMS in all regions must be available.
     * For decrypt, KMS in at least one region must be available.
     */
    protected MasterKeyProvider<KmsMasterKey> initializeKeyProvider(List<String> cmkArns) {
        List<MasterKeyProvider<KmsMasterKey>> providers = cmkArns.stream()
                .map(KmsMasterKeyProvider::new)
                .collect(Collectors.toList());
        return (MasterKeyProvider<KmsMasterKey>) MultipleProviderFactory.buildMultiProvider(providers);
    }
}
