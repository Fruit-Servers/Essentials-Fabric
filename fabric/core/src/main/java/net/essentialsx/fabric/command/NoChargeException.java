package net.essentialsx.fabric.command;

public class NoChargeException extends Exception {
    public NoChargeException() {
        super("Will charge later");
    }
}
