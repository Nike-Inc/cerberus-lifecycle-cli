/*
 * Copyright (c) 2016 Nike Inc.
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
