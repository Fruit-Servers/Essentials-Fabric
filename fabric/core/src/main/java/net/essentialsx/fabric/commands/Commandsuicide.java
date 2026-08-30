package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandsuicide extends EssentialsCommand {
    public Commandsuicide() {
        super("suicide");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        user.getBase().kill();
        if (!user.getBase().isDeadOrDying()) {
            user.getBase().setHealth(0);
        }
        user.sendTl("suicideMessage");
        user.setDisplayNick();
        ess.broadcastTl(user, u -> false, "suicideSuccess", user.getDisplayName());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        return Collections.emptyList();
    }
}
