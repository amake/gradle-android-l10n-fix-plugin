package com.madlonkay.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static final Pattern LOCALE_RESOURCE_PATTERN = Pattern.compile(File.separatorChar + "res" + File.separatorChar + ".*-([a-z]{2}(?:-r[A-Z]{2})?|b(?:\\+[a-zA-Z]+)+)\\b");

    public static String resolveLocale(CharSequence path) {
        Matcher m = LOCALE_RESOURCE_PATTERN.matcher(path);
        if (m.find()) {
            return m.group(1);
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
