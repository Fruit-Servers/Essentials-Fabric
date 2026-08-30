package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Commandtpo extends EssentialsCommand {
    public Commandtpo() {
        super("tpo");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        switch (args.length) {
            case 0:
                throw new NotEnoughArgumentsException();
            case 1:
                final User player = getPlayer(server, user, args, 0);
                if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(player.getWorld()))) {
                    throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(player.getWorld()));
                }
                user.getAsyncTeleport().now(player.getBase(), false, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
                break;
            default:
                if (!user.isAuthorized("essentials.tp.others")) {
                    throw new TranslatableException("noPerm", "essentials.tp.others");
                }
                final User target = getPlayer(server, user, args, 0);
                final User toPlayer = getPlayer(server, user, args, 1);
                if (target.getWorld() != toPlayer.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(toPlayer.getWorld()))) {
                    throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(toPlayer.getWorld()));
                }
                final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
                target.getAsyncTeleport().now(toPlayer.getBase(), false, TeleportCause.COMMAND, future);
                future.thenAccept(success -> {
                    if (success) {
                        target.sendTl("teleportAtoB", user.getDisplayName(), toPlayer.getDisplayName());
                    }
                });
                break;
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 || (args.length == 2 && user.isAuthorized("essentials.tp.others"))) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }
}
