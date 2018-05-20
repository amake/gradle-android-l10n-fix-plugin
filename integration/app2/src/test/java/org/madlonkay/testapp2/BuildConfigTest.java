package org.madlonkay.testapp2;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class BuildConfigTest {
    @Test
    public void supportedLocales() {
        String[] expected = { "en" };
        assertArrayEquals(expected, BuildConfig.SUPPORTED_LOCALES);
    }
}
