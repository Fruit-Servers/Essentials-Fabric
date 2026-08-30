package net.essentialsx.fabric.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Legacy formatting codes (section-sign based) used for compatibility with
 * EssentialsX configuration, nicknames and item names.
 */
public enum ChatColor {
    BLACK('0', "black"),
    DARK_BLUE('1', "dark_blue"),
    DARK_GREEN('2', "dark_green"),
    DARK_AQUA('3', "dark_aqua"),
    DARK_RED('4', "dark_red"),
    DARK_PURPLE('5', "dark_purple"),
    GOLD('6', "gold"),
    GRAY('7', "gray"),
    DARK_GRAY('8', "dark_gray"),
    BLUE('9', "blue"),
    GREEN('a', "green"),
    AQUA('b', "aqua"),
    RED('c', "red"),
    LIGHT_PURPLE('d', "light_purple"),
    YELLOW('e', "yellow"),
    WHITE('f', "white"),
    MAGIC('k', "obfuscated", true),
    BOLD('l', "bold", true),
    STRIKETHROUGH('m', "strikethrough", true),
    UNDERLINE('n', "underline", true),
    ITALIC('o', "italic", true),
    RESET('r', "reset", false);

    public static final char COLOR_CHAR = '§';
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]");
    private static final Map<Character, ChatColor> BY_CHAR = new HashMap<>();

    static {
        for (final ChatColor color : values()) {
            BY_CHAR.put(color.code, color);
        }
    }

    private final char code;
    private final String adventureName;
    private final boolean format;
    private final String toString;

    ChatColor(final char code, final String adventureName) {
        this(code, adventureName, false);
    }

    ChatColor(final char code, final String adventureName, final boolean format) {
        this.code = code;
        this.adventureName = adventureName;
        this.format = format;
        this.toString = new String(new char[] {COLOR_CHAR, code});
    }

    public char getChar() {
        return code;
    }

    public String getAdventureName() {
        return adventureName;
    }

    public boolean isFormat() {
        return format;
    }

    public boolean isColor() {
        return !format && this != RESET;
    }

    @Override
    public String toString() {
        return toString;
    }

    public static ChatColor getByChar(final char code) {
        return BY_CHAR.get(Character.toLowerCase(code));
    }

    public static ChatColor getByChar(final String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return getByChar(code.charAt(0));
    }

    public static String stripColor(final String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String translateAlternateColorCodes(final char altColorChar, final String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static ChatColor valueOfSafe(final String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }
}
