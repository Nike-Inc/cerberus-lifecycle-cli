package com.nike.cerberus.util;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UuidSupplierTest {

    @Test
    public void sanityTestGet() {

        UuidSupplier supplier = new UuidSupplier();

        Set<String> previous = Sets.newHashSet();
        for (int i = 0; i < 1000; i++) {

            String current = supplier.get();

            // not the same
            assertFalse(previous.contains(current));

            // expected length
            assertEquals(36, current.length());

            previous.add(current);
        }
    }

}