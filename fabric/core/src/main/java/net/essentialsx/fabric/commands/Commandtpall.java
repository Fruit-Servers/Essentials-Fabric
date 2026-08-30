package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandtpall extends EssentialsCommand {
    public Commandtpall() {
        super("tpall");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            if (sender.isPlayer()) {
                teleportAllPlayers(server, sender, ess.getUser(sender.getPlayer()), commandLabel);
                return;
            }
            throw new NotEnoughArgumentsException();
        }
        final User target = getPlayer(server, sender, args, 0);
        teleportAllPlayers(server, sender, target, commandLabel);
    }

    private void teleportAllPlayers(final MinecraftServer server, final CommandSource sender, final User target, final String label) {
        sender.sendTl("teleportAll");
        final LazyLocation loc = target.getLocation();
        final boolean senderIsTarget = sender.isPlayer() && sender.getPlayer().equals(target.getBase());
        for (final User player : ess.getOnlineUsers()) {
            if (target == player) {
                continue;
            }
            if (senderIsTarget && target.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !target.isAuthorized("essentials.worlds." + Worlds.permissionName(target.getWorld()))) {
                continue;
            }
            player.getAsyncTeleport().now(loc, false, TeleportCause.COMMAND, getNewExceptionFuture(sender, label));
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
