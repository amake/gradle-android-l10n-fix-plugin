package com.madlonkay.android;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void resolveLocale() {
        assertNull(Util.resolveLocale("/Users/me/project/"));
        assertNull(Util.resolveLocale("/Users/me/project/res/values/strings.xml"));
        assertEquals("ja", Util.resolveLocale("/Users/me/project/res/values-ja/strings.xml"));
        assertEquals("es-rMX", Util.resolveLocale("/Users/me/project/res/values-es-rMX/strings.xml"));
        assertEquals("b+sr+Latn", Util.resolveLocale("/Users/me/project/res/values-b+sr+Latn/strings.xml"));
    }

    @Test
    public void convertToBcp47() {
        assertEquals("ja", Util.toBcp47("ja"));
        assertEquals("es-MX", Util.toBcp47("es-rMX"));
        assertEquals("sr-Latn", Util.toBcp47("b+sr+Latn"));
    }

    @Test
    public void toArrayLiteral() {
        assertEquals("{}", Util.toArrayLiteral(Collections.emptyList()));
        assertEquals("{ \"foo\" }", Util.toArrayLiteral(Collections.singletonList("foo")));
        assertEquals("{ \"foo\", \"bar\" }", Util.toArrayLiteral(Arrays.asList("foo", "bar")));
    }
}
