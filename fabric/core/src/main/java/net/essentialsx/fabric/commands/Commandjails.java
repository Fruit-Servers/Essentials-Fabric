package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;

public class Commandjails extends EssentialsCommand {
    public Commandjails() {
        super("jails");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (ess.getJails().getCount() == 0) {
            sender.sendTl("noJailsDefined");
        } else {
            sender.sendTl("jailList", StringUtil.joinList(" ", ess.getJails().getList().toArray()));
        }
    }
}
