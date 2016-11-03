package com.nike.cerberus.util;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Supplier of UUIDs.
 */
public class UuidSupplier implements Supplier<String> {

    /**
     * Generates a random UUID.
     *
     * @return String representing the UUID
     */
    @Override
    public String get() {
        return UUID.randomUUID().toString();
    }
}