package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandtphere extends EssentialsCommand {
    public Commandtphere() {
        super("tphere");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final User player = getPlayer(server, user, args, 0);
        if (!player.isTeleportEnabled()) {
            throw new TranslatableException("teleportDisabled", player.getDisplayName());
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(user.getWorld()))) {
            throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(user.getWorld()));
        }
        user.getAsyncTeleport().teleportPlayer(player, user.getBase(), new Trade(this.getName(), ess), TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }
}
