package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsToggleCommand;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.minecraft.server.MinecraftServer;

public class Commandgod extends EssentialsToggleCommand {
    public Commandgod() {
        super("god", "essentials.god.others");
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
            enabled = !user.isGodModeEnabled();
        }
        user.setGodModeEnabled(enabled);
        if (enabled && user.getBase().getHealth() != 0) {
            user.getBase().setHealth(user.getBase().getMaxHealth());
            user.getBase().getFoodData().setFoodLevel(20);
        }
        user.sendTl("godMode", CommonPlaceholders.enableDisable(user.getSource(), enabled));
        if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
            sender.sendTl("godMode", CommonPlaceholders.enableDisable(user.getSource(), enabled));
        }
    }
}
