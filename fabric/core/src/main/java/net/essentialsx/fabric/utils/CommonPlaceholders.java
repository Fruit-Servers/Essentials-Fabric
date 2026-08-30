package net.essentialsx.fabric.utils;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.text.Text;

public final class CommonPlaceholders {
    private CommonPlaceholders() {
    }

    public static Text.ParsedPlaceholder enableDisable(final CommandSource source, final boolean enable) {
        return Text.parsed(source.tl(enable ? "enabled" : "disabled"));
    }

    public static Text.ParsedPlaceholder trueFalse(final CommandSource source, final boolean condition) {
        return Text.parsed(source.tl(condition ? "true" : "false"));
    }
}
