package net.essentialsx.fabric.command;

import net.essentialsx.fabric.text.TranslatableException;

public class PlayerNotFoundException extends TranslatableException {
    public PlayerNotFoundException() {
        super("playerNotFound");
    }
}
