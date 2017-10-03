package com.nike.cerberus.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * Generate a Salt
 */
public class SaltGenerator {

    private Random random = new SecureRandom();

    /**
     * Generate a Salt as a Base64 encoded String we can store in a properties file
     */
    public String generateSalt() {
        byte[] salt = new byte[64];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
