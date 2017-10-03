package com.nike.cerberus.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SaltGeneratorTest {

    @Test
    public void sanityTestSalt() {
        SaltGenerator generator = new SaltGenerator();
        String s1 = generator.generateSalt();
        String s2 = generator.generateSalt();
        assertFalse(StringUtils.equals(s1, s2));
        assertEquals(88, s1.length());
        assertEquals(88, s2.length());
    }
}