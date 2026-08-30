package net.essentialsx.fabric.command;

import net.essentialsx.fabric.text.TranslatableException;

public class InvalidModifierException extends TranslatableException {
    public InvalidModifierException() {
        super("invalidNumber");
    }
}
