package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandtpahere extends EssentialsCommand {
    public Commandtpahere() {
        super("tpahere");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User player = getPlayer(server, user, args, 0);
        if (user.getName().equalsIgnoreCase(player.getName())) {
            throw new NotEnoughArgumentsException();
        }
        if (!player.isAuthorized("essentials.tpaccept")) {
            throw new TranslatableException("teleportNoAcceptPermission", player.getDisplayName());
        }
        if (!player.isTeleportEnabled()) {
            throw new TranslatableException("teleportDisabled", player.getDisplayName());
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(user.getWorld()))) {
            throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(user.getWorld()));
        }
        // Don't let sender request teleport twice to the same player.
        if (player.hasOutstandingTpaRequest(user.getName(), true)) {
            throw new TranslatableException("requestSentAlready", player.getDisplayName());
        }
        if (!player.isIgnoredPlayer(user)) {
            player.requestTeleport(user, true);
            player.sendTl("teleportHereRequest", user.getDisplayName());
            player.sendTl("typeTpaccept");
            player.sendTl("typeTpdeny");
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendTl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation());
            }
        }
        user.sendTl("requestSent", player.getDisplayName());
        user.sendTl("typeTpacancel");
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
