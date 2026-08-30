package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.rtp.RandomTeleport;
import net.essentialsx.fabric.teleport.TeleportCause;

import java.util.concurrent.CompletableFuture;

public class SignRandomTeleport extends EssentialsSign {
    public SignRandomTeleport() {
        super("RandomTeleport");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException, MaxMoneyException {
        final String name = sign.getLine(1);
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        randomTeleport.getRandomLocation(name).thenAccept(location -> {
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.thenAccept(success -> {
                if (success) {
                    player.sendTl("tprSuccess");
                }
            });
            player.getAsyncTeleport().now(location, false, TeleportCause.COMMAND, future);
        }).exceptionally(t -> {
            ess.showError(player.getSource(), t, "\\ sign: " + signName);
            return null;
        });
        return true;
    }
}
