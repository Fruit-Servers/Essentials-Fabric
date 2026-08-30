package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsToggleCommand;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

public class Commandtpauto extends EssentialsToggleCommand {
    public Commandtpauto() {
        super("tpauto", "essentials.tpauto.others");
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
            enabled = !user.isAutoTeleportEnabled();
        }
        user.setAutoTeleportEnabled(enabled);
        user.sendTl(enabled ? "autoTeleportEnabled" : "autoTeleportDisabled");
        if (enabled && !user.isTeleportEnabled()) {
            user.sendTl("teleportationDisabledWarning");
        }
        if (!sender.isPlayer() || !user.getBase().equals(sender.getPlayer())) {
            sender.sendTl(enabled ? "autoTeleportEnabledFor" : "autoTeleportDisabledFor", user.getDisplayName());
        }
    }
}
