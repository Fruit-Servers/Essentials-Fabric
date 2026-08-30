package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandworkbench extends EssentialsCommand {
    public Commandworkbench() {
        super("workbench");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        Workstations.openWorkbench(user.getBase());
    }
}
