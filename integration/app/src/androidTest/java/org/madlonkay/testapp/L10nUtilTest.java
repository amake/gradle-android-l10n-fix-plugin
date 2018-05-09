package org.madlonkay.testapp;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class L10nUtilTest {

    @Test
    public void isSupportedLocale() {
        {
            List<Locale> supported = Collections.singletonList(Locale.forLanguageTag("en"));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("en"), supported));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("en-US"), supported));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("en-UK"), supported));
        }
        {
            List<Locale> supported = Arrays.asList(Locale.forLanguageTag("es"), Locale.forLanguageTag("en"));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("en"), supported));
        }
        {
            List<Locale> supported = Collections.singletonList(Locale.forLanguageTag("zh-CN"));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh"), supported));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh-Hans"), supported));
            assertFalse(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh-Hant"), supported));
        }
        {
            List<Locale> supported = Collections.singletonList(Locale.forLanguageTag("zh-HK"));
            assertFalse(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh"), supported));
            assertFalse(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh-Hans"), supported));
            assertTrue(L10nUtil.isSupportedLocale(Locale.forLanguageTag("zh-Hant"), supported));
        }
    }
}