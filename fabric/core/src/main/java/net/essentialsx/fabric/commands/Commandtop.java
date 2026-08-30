package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;

public class Commandtop extends EssentialsCommand {
    public Commandtop() {
        super("top");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final int topX = user.getLocation().blockX();
        final int topZ = user.getLocation().blockZ();
        final float pitch = user.getLocation().pitch();
        final float yaw = user.getLocation().yaw();
        final LazyLocation unsafe = LazyLocation.of(user.getWorld(), topX, user.getWorld().getMaxBuildHeight(), topZ, yaw, pitch);
        final LazyLocation safe = LocationUtil.getSafeDestination(ess, unsafe);
        final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
        future.thenAccept(success -> {
            if (success) {
                user.sendTl("teleportTop", safe.worldDisplayName(server), safe.blockX(), safe.blockY(), safe.blockZ());
            }
        });
        user.getAsyncTeleport().teleport(safe, new Trade(this.getName(), ess), TeleportCause.COMMAND, future);
    }
}
