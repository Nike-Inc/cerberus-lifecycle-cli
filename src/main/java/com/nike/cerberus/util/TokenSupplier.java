package com.nike.cerberus.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;

/**
 * Supplier for a Base64 encoded string of a random 16 bytes.
 */
public class TokenSupplier implements Supplier<String> {

    SecureRandom secureRandom = new SecureRandom();

    /**
     * Supplies a Base64 encoded string of a random 16 bytes.
     *
     * @return Base64 encoded string
     */
    @Override
    public String get() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
