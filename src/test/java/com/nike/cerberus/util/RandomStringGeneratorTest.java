package com.nike.cerberus.util;

import com.beust.jcommander.internal.Sets;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RandomStringGeneratorTest {

    @Test
    public void sanityTestGet() {

        RandomStringGenerator generator = new RandomStringGenerator();

        Set<String> previous = Sets.newHashSet();
        for (int i = 0; i < 1000; i++) {

            String current = generator.get();

            // expecting unique Strings
            assertFalse(previous.contains(current));

            // produces 30 or less characters
            assertTrue("failed on " + current, current.length() <= 30);

            // should never be empty
            assertFalse(current.isEmpty());

            previous.add(current);
        }
    }

}