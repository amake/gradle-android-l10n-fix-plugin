package org.madlonkay.testapp;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class BuildConfigTest {
    @Test
    public void supportedLocales() {
        String[] expected = { "cs", "de", "en", "es-MX", "fr-FR", "ja", "ko", "sr-Latn" };
        assertArrayEquals(expected, BuildConfig.SUPPORTED_LOCALES);
        assertArrayEquals(expected, org.madlonkay.testlibrary.BuildConfig.SUPPORTED_LOCALES);
        assertArrayEquals(expected, org.madlonkay.testlibrary2.BuildConfig.SUPPORTED_LOCALES);
    }
}
