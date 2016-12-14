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
