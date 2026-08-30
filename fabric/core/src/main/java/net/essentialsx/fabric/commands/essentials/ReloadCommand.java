package net.essentialsx.fabric.commands.essentials;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsTreeNode;

public class ReloadCommand extends EssentialsTreeNode {
    public ReloadCommand() {
        super("reload");
    }

    @Override
    protected void run(final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        ess.reload();
        sender.sendTl("essentialsReload", VersionCommand.essentialsVersion());
    }
}
