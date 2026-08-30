package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;

import java.util.Set;
import java.util.TreeSet;

/**
 * Lists Essentials commands that are disabled in config (and therefore left to vanilla/other mods),
 * the Fabric analogue of upstream's overridden command map listing.
 */
public class CommandMapCommand extends EssentialsTreeNode {
    public CommandMapCommand() {
        super("cmd", "commands");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final Set<String> disabled = new TreeSet<>(ess.getSettings().getDisabledCommands());
        if (disabled.isEmpty()) {
            sender.sendTl("blockListEmpty");
            return;
        }
        sender.sendTl("blockList");
        for (final String entry : disabled) {
            sender.sendMessage(entry + " => (disabled, falls through to vanilla/other mods)");
        }
    }
}
