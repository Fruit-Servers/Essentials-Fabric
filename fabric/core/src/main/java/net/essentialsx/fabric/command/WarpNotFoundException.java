package net.essentialsx.fabric.command;

import net.essentialsx.fabric.text.TranslatableException;

public class WarpNotFoundException extends TranslatableException {
    public WarpNotFoundException() {
        super("warpNotExist");
    }
}
