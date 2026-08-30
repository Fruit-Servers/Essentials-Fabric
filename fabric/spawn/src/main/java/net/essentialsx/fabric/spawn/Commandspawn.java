package net.essentialsx.fabric.spawn;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Commandspawn extends EssentialsCommand {
    private final SpawnStorage spawns;

    public Commandspawn(final SpawnStorage spawns) {
        super("spawn");
        this.spawns = spawns;
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        if (args.length > 0 && user.isAuthorized("essentials.spawn.others")) {
            final User otherUser = getPlayer(server, user, args, 0);
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.thenAccept(success -> {
                if (success) {
                    if (!otherUser.equals(user)) {
                        otherUser.sendTl("teleportAtoB", user.getDisplayName(), "spawn");
                    }
                }
            });
            respawn(user.getSource(), user, otherUser, charge, commandLabel, future);
        } else {
            respawn(user.getSource(), user, user, charge, commandLabel, new CompletableFuture<>());
        }
        throw new NoChargeException();
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, args, 0, true, false);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        respawn(sender, null, user, null, commandLabel, future);
        future.thenAccept(success -> {
            if (success) {
                user.sendTl("teleportAtoB", Console.displayName(), "spawn");
            }
        });
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.spawn.others")) {
            return getPlayers(sender);
        }
        return Collections.emptyList();
    }

    private void respawn(final CommandSource sender, final User teleportOwner, final User teleportee, final Trade charge, final String commandLabel, final CompletableFuture<Boolean> future) throws Exception {
        final LazyLocation spawn = spawns.getSpawn(teleportee.getGroup());
        if (spawn == null) {
            return;
        }
        future.exceptionally(e -> {
            showError(sender, e, commandLabel);
            return false;
        });
        if (teleportOwner == null) {
            teleportee.getAsyncTeleport().now(spawn, false, TeleportCause.COMMAND, future);
        } else {
            teleportOwner.getAsyncTeleport().teleportPlayer(teleportee, spawn, charge, TeleportCause.COMMAND, future);
        }
        future.thenAccept(success -> {
            if (success) {
                sender.sendTl("teleporting", spawn.worldDisplayName(ess.getServer()), spawn.blockX(), spawn.blockY(), spawn.blockZ());
            }
        });
    }
}
