package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Commandtp extends EssentialsCommand {
    public Commandtp() {
        super("tp");
    }

    static double relative(final String arg, final double base) {
        return arg.startsWith("~") ? base + (arg.length() > 1 ? Double.parseDouble(arg.substring(1)) : 0) : Double.parseDouble(arg);
    }

    static boolean outOfBounds(final double x, final double y, final double z) {
        return x > 30000000 || y > 30000000 || z > 30000000 || x < -30000000 || y < -30000000 || z < -30000000;
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
        switch (args.length) {
            case 0:
                throw new NotEnoughArgumentsException();
            case 1:
                final User player = getPlayer(server, user, args, 0, true);
                if (!player.isTeleportEnabled()) {
                    throw new TranslatableException("teleportDisabled", player.getDisplayName());
                }
                if (!player.isOnline()) {
                    if (user.isAuthorized("essentials.tpoffline")) {
                        throw new TranslatableException("teleportOffline", player.getDisplayName());
                    }
                    throw new PlayerNotFoundException();
                }
                if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(player.getWorld()))) {
                    throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(player.getWorld()));
                }
                final Trade charge = new Trade(this.getName(), ess);
                charge.isAffordableFor(user);
                user.getAsyncTeleport().teleport(player.getBase(), charge, TeleportCause.COMMAND, future);
                throw new NoChargeException();
            case 3:
                if (!user.isAuthorized("essentials.tp.position")) {
                    throw new TranslatableException("noPerm", "essentials.tp.position");
                }
                final double x2 = relative(args[0], user.getLocation().x());
                final double y2 = relative(args[1], user.getLocation().y());
                final double z2 = relative(args[2], user.getLocation().z());
                if (outOfBounds(x2, y2, z2)) {
                    throw new NotEnoughArgumentsException(user.playerTl("teleportInvalidLocation"));
                }
                final LazyLocation locpos = LazyLocation.of(user.getWorld(), x2, y2, z2, user.getLocation().yaw(), user.getLocation().pitch());
                user.getAsyncTeleport().now(locpos, true, TeleportCause.COMMAND, future);
                future.thenAccept(success -> {
                    if (success) {
                        user.sendTl("teleporting", locpos.worldDisplayName(server), locpos.blockX(), locpos.blockY(), locpos.blockZ());
                    }
                });
                break;
            case 4:
                if (!user.isAuthorized("essentials.tp.others")) {
                    throw new TranslatableException("noPerm", "essentials.tp.others");
                }
                if (!user.isAuthorized("essentials.tp.position")) {
                    throw new TranslatableException("noPerm", "essentials.tp.position");
                }
                final User target2 = getPlayer(server, user, args, 0);
                final double x = relative(args[1], target2.getLocation().x());
                final double y = relative(args[2], target2.getLocation().y());
                final double z = relative(args[3], target2.getLocation().z());
                if (outOfBounds(x, y, z)) {
                    throw new NotEnoughArgumentsException(user.playerTl("teleportInvalidLocation"));
                }
                final LazyLocation locposother = LazyLocation.of(target2.getWorld(), x, y, z, target2.getLocation().yaw(), target2.getLocation().pitch());
                if (!target2.isTeleportEnabled()) {
                    throw new TranslatableException("teleportDisabled", target2.getDisplayName());
                }
                user.sendTl("teleporting", locposother.worldDisplayName(server), locposother.blockX(), locposother.blockY(), locposother.blockZ());
                target2.getAsyncTeleport().now(locposother, false, TeleportCause.COMMAND, future);
                future.thenAccept(success -> {
                    if (success) {
                        target2.sendTl("teleporting", locposother.worldDisplayName(server), locposother.blockX(), locposother.blockY(), locposother.blockZ());
                    }
                });
                break;
            case 2:
            default:
                if (!user.isAuthorized("essentials.tp.others")) {
                    throw new TranslatableException("noPerm", "essentials.tp.others");
                }
                final User target = getPlayer(server, user, args, 0);
                final User toPlayer = getPlayer(server, user, args, 1);
                if (!target.isTeleportEnabled()) {
                    throw new TranslatableException("teleportDisabled", target.getDisplayName());
                }
                if (!toPlayer.isTeleportEnabled()) {
                    throw new TranslatableException("teleportDisabled", toPlayer.getDisplayName());
                }
                if (target.getWorld() != toPlayer.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(toPlayer.getWorld()))) {
                    throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(toPlayer.getWorld()));
                }
                target.sendTl("teleportAtoB", user.getDisplayName(), toPlayer.getDisplayName());
                target.getAsyncTeleport().now(toPlayer.getBase(), false, TeleportCause.COMMAND, future);
                break;
        }
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final User target = getPlayer(server, args, 0, true, false);
        if (args.length == 2) {
            final User toPlayer = getPlayer(server, args, 1, true, false);
            target.sendTl("teleportAtoB", Console.displayName(), toPlayer.getDisplayName());
            target.getAsyncTeleport().now(toPlayer.getBase(), false, TeleportCause.COMMAND, getNewExceptionFuture(sender, commandLabel));
        } else if (args.length > 3) {
            final double x = relative(args[1], target.getLocation().x());
            final double y = relative(args[2], target.getLocation().y());
            final double z = relative(args[3], target.getLocation().z());
            if (outOfBounds(x, y, z)) {
                throw new NotEnoughArgumentsException(sender.tl("teleportInvalidLocation"));
            }
            final LazyLocation loc = LazyLocation.of(target.getWorld(), x, y, z, target.getLocation().yaw(), target.getLocation().pitch());
            sender.sendTl("teleporting", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
            final CompletableFuture<Boolean> future = getNewExceptionFuture(sender, commandLabel);
            target.getAsyncTeleport().now(loc, false, TeleportCause.COMMAND, future);
            future.thenAccept(success -> {
                if (success) {
                    target.sendTl("teleporting", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
                }
            });
        } else {
            throw new NotEnoughArgumentsException();
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

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 || args.length == 2) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
