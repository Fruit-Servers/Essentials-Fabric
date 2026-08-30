package net.essentialsx.fabric.command;

public class QuietAbortException extends Exception {
    public QuietAbortException() {
        super();
    }

    public QuietAbortException(final String message) {
        super(message);
    }
}
