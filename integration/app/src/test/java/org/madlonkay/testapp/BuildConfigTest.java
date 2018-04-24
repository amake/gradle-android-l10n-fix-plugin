package org.madlonkay.testapp;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;

public class BuildConfigTest {
    @Test
    public void supportedLocales() {
        String[] expected = { "de", "en", "es-MX", "fr", "ja", "sr-Latn" };
        assertArrayEquals(expected, BuildConfig.SUPPORTED_LOCALES);
        assertArrayEquals(expected, org.madlonkay.testlibrary.BuildConfig.SUPPORTED_LOCALES);
    }
}