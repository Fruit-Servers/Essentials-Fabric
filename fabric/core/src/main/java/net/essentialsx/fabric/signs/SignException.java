package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.text.TranslatableException;

public class SignException extends TranslatableException {
    public SignException(final String tlKey, final Object... args) {
        super(tlKey, args);
    }

    public SignException(final Throwable cause, final String tlKey, final Object... args) {
        super(cause, tlKey, args);
    }
}
