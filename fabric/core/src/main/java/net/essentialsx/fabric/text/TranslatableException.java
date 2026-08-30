package net.essentialsx.fabric.text;

/**
 * An exception whose message is resolved through the locale system at display time.
 */
public class TranslatableException extends Exception {
    private final String tlKey;
    private final Object[] args;

    public TranslatableException(final String tlKey, final Object... args) {
        super(I18n.tlLiteral(tlKey, args));
        this.tlKey = tlKey;
        this.args = args;
    }

    public TranslatableException(final Throwable cause, final String tlKey, final Object... args) {
        super(I18n.tlLiteral(tlKey, args), cause);
        this.tlKey = tlKey;
        this.args = args;
    }

    public String getTlKey() {
        return tlKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
