package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandpowertooltoggle extends EssentialsCommand {
    public Commandpowertooltoggle() {
        super("powertooltoggle");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.hasPowerTools()) {
            user.sendTl("noPowerTools");
            return;
        }
        user.sendTl(user.togglePowerToolsEnabled() ? "powerToolsEnabled" : "powerToolsDisabled");
    }
}
