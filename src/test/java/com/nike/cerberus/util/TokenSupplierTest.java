package com.nike.cerberus.util;

import com.beust.jcommander.internal.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TokenSupplierTest {

    @Test
    public void sanityTestGet() {

        TokenSupplier supplier = new TokenSupplier();

        Set<String> previous = Sets.newHashSet();
        for (int i = 0; i < 1000; i++) {
            String current = supplier.get();

            // not the same
            assertFalse(previous.contains(current));

            // check length
            assertEquals(24, current.length());

            previous.add(current);
        }
    }

}