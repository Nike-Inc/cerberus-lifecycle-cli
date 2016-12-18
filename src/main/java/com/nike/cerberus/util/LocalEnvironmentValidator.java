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

package com.nike.cerberus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;

/**
 * Validation of local environment where CLI is installed.
 */
public class LocalEnvironmentValidator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Validate local environment where CLI is installed.
     */
    public void validate() {
        validateUnlimitedStrengthEncryptionInstalled();
    }

    /**
     * Throw exception if "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy" is not installed.
     * This allows the CLI to fail early.
     */
    private void validateUnlimitedStrengthEncryptionInstalled() {
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") <= 256) {
                throw new RuntimeException("Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy is required but was not found");
            }
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Tried to validate UnlimitedEncryption was installed", e);
        }
    }
}
