package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsToggleCommand;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.minecraft.server.MinecraftServer;

public class Commandfly extends EssentialsToggleCommand {
    public Commandfly() {
        super("fly", "essentials.fly.others");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        toggleOtherPlayers(server, sender, args);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        handleToggleWithArgs(server, user, args);
    }

    @Override
    protected void togglePlayer(final CommandSource sender, final User user, Boolean enabled) {
        if (enabled == null) {
            enabled = !user.getBase().getAbilities().mayfly;
        }
        user.getBase().resetFallDistance();
        user.getBase().getAbilities().mayfly = enabled;
        user.setFlyModeEnabled(enabled);
        if (!enabled) {
            user.getBase().getAbilities().flying = false;
        }
        user.getBase().onUpdateAbilities();
        user.sendTl("flyMode", CommonPlaceholders.enableDisable(user.getSource(), enabled), user.getDisplayName());
        if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
            sender.sendTl("flyMode", CommonPlaceholders.enableDisable(user.getSource(), enabled), user.getDisplayName());
        }
    }
}
