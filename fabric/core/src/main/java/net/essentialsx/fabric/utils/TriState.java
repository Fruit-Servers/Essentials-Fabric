package net.essentialsx.fabric.utils;

public enum TriState {
    TRUE,
    FALSE,
    UNSET;

    public static TriState of(final boolean value) {
        return value ? TRUE : FALSE;
    }

    public static TriState of(final net.fabricmc.fabric.api.util.TriState fabric) {
        return switch (fabric) {
            case TRUE -> TRUE;
            case FALSE -> FALSE;
            default -> UNSET;
        };
    }

    public boolean asBoolean(final boolean def) {
        return this == UNSET ? def : this == TRUE;
    }
}
