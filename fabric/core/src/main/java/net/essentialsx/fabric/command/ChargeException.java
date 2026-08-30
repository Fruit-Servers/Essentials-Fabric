package net.essentialsx.fabric.command;

import net.essentialsx.fabric.text.TranslatableException;

public class ChargeException extends TranslatableException {
    public ChargeException(final String tlKey, final Object... args) {
        super(tlKey, args);
    }

    public ChargeException(final Throwable cause, final String tlKey, final Object... args) {
        super(cause, tlKey, args);
    }
}
