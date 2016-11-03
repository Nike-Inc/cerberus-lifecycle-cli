package com.nike.cerberus.cli;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

public class CerberusRunnerTest {

    @Test
    public void sanityTestHelp() {

        // This is a little gross but it does help prove basic start-up works.
        // It will be a problem if we ever run tests in parallel.

        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            // invoke method under test
            CerberusRunner.main(new String[]{"--help"});

            String output = out.toString();

            // limited validation of help output
            assertContains(output, "Usage: cerberus [options] [command] [command options]");
            assertContains(output, "--debug");
            assertContains(output, "--environment");
            assertContains(output, "--help");

            // sanity test that output is about expected length
            assertTrue(output.length() >= 9000);

        } finally {
            // restore System.out
            System.setOut(originalOut);
        }
    }

    /**
     * Assert a String contains expectedContents
     *
     * @param stringToCheck    String to validate it has expectedContents
     * @param expectedContents the substring we expect to see in stringToCheck
     */
    private void assertContains(String stringToCheck, String expectedContents) {
        assertTrue("did not find expected contents: " + expectedContents, stringToCheck.contains(expectedContents));
    }
}