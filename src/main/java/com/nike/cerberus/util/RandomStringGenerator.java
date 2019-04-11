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
