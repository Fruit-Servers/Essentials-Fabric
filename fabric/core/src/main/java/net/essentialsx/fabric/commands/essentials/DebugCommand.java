package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DebugCommand extends EssentialsTreeNode {
    public DebugCommand() {
        super("debug", "verbose");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final boolean newDebugState;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("true")) {
                newDebugState = true;
            } else if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("false")) {
                newDebugState = false;
            } else {
                newDebugState = !ess.getSettings().isDebug();
            }
        } else {
            newDebugState = !ess.getSettings().isDebug();
        }
        ess.getSettings().setDebug(newDebugState);
        sender.sendMessage("Essentials " + VersionCommand.essentialsVersion() + " debug mode " + (ess.getSettings().isDebug() ? "enabled" : "disabled"));
    }

    @Override
    protected List<String> tabComplete(final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off");
        }
        return Collections.emptyList();
    }
}
