package net.essentialsx.fabric.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class StringUtil {
    private static final Pattern INVALIDFILECHARS = Pattern.compile("[^a-z0-9-]");
    private static final Pattern STRICTINVALIDCHARS = Pattern.compile("[^a-z0-9]");
    private static final Pattern INVALIDCHARS = Pattern.compile("[^\t\n\r -~ -퟿-￼]");
    private static final Pattern WINDOWS_RESERVED = Pattern.compile("^(con|prn|aux|nul|com[0-9]|lpt[0-9])$");

    private StringUtil() {
    }

    public static String sanitizeFileName(final String name) {
        return INVALIDFILECHARS.matcher(name.toLowerCase(Locale.ENGLISH)).replaceAll("_");
    }

    public static boolean isReservedFileName(final String name) {
        return WINDOWS_RESERVED.matcher(name.toLowerCase(Locale.ENGLISH)).matches();
    }

    public static String safeString(final String string) {
        if (string == null) {
            return null;
        }
        return STRICTINVALIDCHARS.matcher(string.toLowerCase(Locale.ENGLISH)).replaceAll("_");
    }

    public static String sanitizeString(final String string) {
        return INVALIDCHARS.matcher(string).replaceAll("");
    }

    public static String joinList(final Object... list) {
        return joinList(", ", list);
    }

    public static String joinList(final String seperator, final Object... list) {
        final StringBuilder buf = new StringBuilder();
        for (final Object each : list) {
            if (buf.length() > 0) {
                buf.append(seperator);
            }
            if (each instanceof Collection) {
                buf.append(joinList(seperator, ((Collection<?>) each).toArray()));
            } else {
                buf.append(each);
            }
        }
        return buf.toString();
    }

    public static String joinListSkip(final String seperator, final String skip, final Object... list) {
        final StringBuilder buf = new StringBuilder();
        for (final Object each : list) {
            if (each.toString().equalsIgnoreCase(skip)) {
                continue;
            }
            if (buf.length() > 0) {
                buf.append(seperator);
            }
            if (each instanceof Collection) {
                buf.append(joinListSkip(seperator, skip, ((Collection<?>) each).toArray()));
            } else {
                buf.append(each);
            }
        }
        return buf.toString();
    }

    public static UUID toUUID(final String input) {
        try {
            return UUID.fromString(input);
        } catch (final IllegalArgumentException ignored) {
        }
        return null;
    }

    public static String abbreviate(final String input, final int length) {
        if (input == null) return null;
        if (length < 4) throw new IllegalArgumentException("Invalid length " + length);
        if (input.length() <= length) return input;
        return input.substring(0, length - 3) + "...";
    }

    public static String stripToNull(final String input) {
        if (input == null) return null;
        final String result = strip(input);
        return result.isEmpty() ? null : result;
    }

    public static String strip(final String input) {
        return strip(input, Character::isWhitespace);
    }

    public static String strip(final String input, final String stripChars) {
        if (stripChars == null) return strip(input);
        return strip(input, c -> stripChars.indexOf(c) >= 0);
    }

    public static String strip(final String input, final Function<Character, Boolean> shouldStrip) {
        if (input == null) return null;
        int startIndex = 0;
        int endIndex = input.length();
        for (; startIndex < endIndex; startIndex++) {
            if (!shouldStrip.apply(input.charAt(startIndex))) break;
        }
        for (; endIndex > startIndex; endIndex--) {
            if (!shouldStrip.apply(input.charAt(endIndex - 1))) break;
        }
        return input.substring(startIndex, endIndex);
    }

    /**
     * Bukkit StringUtil#copyPartialMatches equivalent.
     */
    public static List<String> copyPartialMatches(final String token, final Iterable<String> originals, final List<String> collection) {
        for (final String string : originals) {
            if (startsWithIgnoreCase(string, token)) {
                collection.add(string);
            }
        }
        return collection;
    }

    public static List<String> partialMatches(final String token, final Iterable<String> originals) {
        return copyPartialMatches(token, originals, new ArrayList<>());
    }

    public static boolean startsWithIgnoreCase(final String string, final String prefix) {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
