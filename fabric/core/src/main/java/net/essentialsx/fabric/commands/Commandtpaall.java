package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandtpaall extends EssentialsCommand {
    public Commandtpaall() {
        super("tpaall");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            if (sender.isPlayer()) {
                tpaAll(sender, ess.getUser(sender.getPlayer()));
                return;
            }
            throw new NotEnoughArgumentsException();
        }
        final User target = getPlayer(server, sender, args, 0);
        tpaAll(sender, target);
    }

    private void tpaAll(final CommandSource sender, final User target) {
        sender.sendTl("teleportAAll");
        final boolean senderIsTarget = sender.isPlayer() && sender.getPlayer().equals(target.getBase());
        for (final User player : ess.getOnlineUsers()) {
            if (target == player) {
                continue;
            }
            if (!player.isTeleportEnabled()) {
                continue;
            }
            if (senderIsTarget && target.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !target.isAuthorized("essentials.worlds." + Worlds.permissionName(target.getWorld()))) {
                continue;
            }
            try {
                player.requestTeleport(target, true);
                player.sendTl("teleportHereRequest", target.getDisplayName());
                player.sendTl("typeTpaccept");
                if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                    player.sendTl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation());
                }
            } catch (final Exception ex) {
                ess.showError(sender, ex, getName());
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
