package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Workstations;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandloom extends EssentialsCommand {
    public Commandloom() {
        super("loom");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        Workstations.openLoom(user.getBase());
    }
}
