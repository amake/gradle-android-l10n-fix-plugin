package com.madlonkay.android;

import com.android.utils.StringHelper;

import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Util {

    private static final String LOCALE_PREFIX = "values-";
    private static final Pattern LOCALE_RESOURCE_PATTERN = Pattern.compile("[a-z]{2}(?:-r[A-Z]{2})?|b(?:\\+[a-zA-Z]+)+");

    public static boolean isLocaleQualifier(String s) {
        return LOCALE_RESOURCE_PATTERN.matcher(s).matches();
    }

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

    public static <T> void transformInto(Collection<T> source, Function<T, T> transform, Collection<T> target) {
        for (T item : source) {
            target.add(transform.apply(item));
        }
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

    static String makeTaskName(String prefix, String... words) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String word : words) {
            StringHelper.appendCapitalized(sb, word);
        }
        return sb.toString();
    }

    static int readIntProperty(Project project, String key, int defaultValue) {
        Object rawValue = project.findProperty(key);
        int result = defaultValue;
        if (rawValue != null) {
            if (rawValue instanceof Number) {
                result = ((Number) rawValue).intValue();
            } else {
                try {
                    result = Integer.parseInt(rawValue.toString());
                } catch (NumberFormatException ex) {
                    project.getLogger().warn("Could not parse property {}={} as integer", key, rawValue);
                }
            }
        }
        return result;
    }
}
