package com.madlonkay.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {

    private static final String LOCALE_PREFIX = "values-";

    public static String resolveLocale(File file) {
        String result = resolveLocale(file.getName());
        if (result == null) {
            result = resolveLocale(file.getParentFile().getName());
        }
        return result;
    }

    private static String resolveLocale(String path) {
        if (path.startsWith(LOCALE_PREFIX)) {
            return path.substring(LOCALE_PREFIX.length());
        } else {
            return null;
        }
    }

    public static Set<String> toBcp47(Collection<String> resLocales) {
        Set<String> result = new HashSet<>(resLocales.size());
        for (String resLocale : resLocales) {
            result.add(toBcp47(resLocale));
        }
        return result;
    }

    public static String toBcp47(String resLocale) {
        return resLocale.replace("b+", "").replace("-r", "-").replace('+', '-');
    }

    public static String toArrayLiteral(Collection<?> items) {
        if (items.isEmpty()) {
            return "{}";
        }
        List<String> quoted = new ArrayList<>(items.size());
        for (Object item : items) {
            quoted.add('"' + item.toString() + '"');
        }
        return "{ " + String.join(", ", quoted) + " }";
    }
}
