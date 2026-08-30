package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commanddepth extends EssentialsCommand {
    public Commanddepth() {
        super("depth");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final int depth = user.getLocation().blockY() - user.getWorld().getSeaLevel();
        if (depth > 0) {
            user.sendTl("depthAboveSea", depth);
        } else if (depth < 0) {
            user.sendTl("depthBelowSea", -depth);
        } else {
            user.sendTl("depth");
        }
    }
}
