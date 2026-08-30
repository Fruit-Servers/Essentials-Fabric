package net.essentialsx.fabric.command;

import net.essentialsx.fabric.text.TranslatableException;

public class PlayerExemptException extends TranslatableException {
    public PlayerExemptException(final String tlKey, final Object... args) {
        super(tlKey, args);
    }
}
