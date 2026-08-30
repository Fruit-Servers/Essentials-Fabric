package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;

public class Commandbottom extends EssentialsCommand {
    public Commandbottom() {
        super("bottom");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final LazyLocation current = user.getLocation();
        final LazyLocation unsafe = current.withPosition(current.blockX(), user.getWorld().getMinBuildHeight(), current.blockZ());
        final LazyLocation safe = LocationUtil.getSafeDestination(ess, unsafe);
        final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
        future.thenAccept(success -> {
            if (success) {
                user.sendTl("teleportBottom", safe.worldDisplayName(ess.getServer()), safe.blockX(), safe.blockY(), safe.blockZ());
            }
        });
        user.getAsyncTeleport().teleport(safe, new Trade(this.getName(), ess), TeleportCause.COMMAND, future);
    }
}
