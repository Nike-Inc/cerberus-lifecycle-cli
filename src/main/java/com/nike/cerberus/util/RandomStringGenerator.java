package com.nike.cerberus.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.function.Supplier;

/**
 * Generates random strings using {@link SecureRandom}.
 */
public class RandomStringGenerator implements Supplier<String> {

    private static final int MAX_BIT_LENGTH = 150;

    private static final int RADIX = 32;

    private final int maxBitLength;

    private final int radix;

    private final SecureRandom secureRandom = new SecureRandom();

    public RandomStringGenerator() {
        this(MAX_BIT_LENGTH, RADIX);
    }

    public RandomStringGenerator(final int maxBitLength, final int radix) {
        this.maxBitLength = maxBitLength;
        this.radix = radix;
    }

    @Override
    public String get() {
        return new BigInteger(maxBitLength, secureRandom).toString(radix);
    }
}