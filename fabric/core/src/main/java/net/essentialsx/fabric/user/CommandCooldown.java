package net.essentialsx.fabric.user;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CommandCooldown {
    private final Pattern pattern;
    private final long value;

    public CommandCooldown(final Pattern pattern, final long value) {
        this.pattern = pattern;
        this.value = value;
    }

    public static CommandCooldown fromMap(final Map<String, Object> map) {
        final Object pattern = map.get("pattern");
        final Object value = map.get("value");
        if (pattern == null || !(value instanceof Number)) {
            return null;
        }
        try {
            return new CommandCooldown(Pattern.compile(pattern.toString()), ((Number) value).longValue());
        } catch (final PatternSyntaxException ex) {
            return null;
        }
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("pattern", pattern.pattern());
        map.put("value", value);
        return map;
    }

    public Pattern pattern() {
        return pattern;
    }

    public long value() {
        return value;
    }
}
