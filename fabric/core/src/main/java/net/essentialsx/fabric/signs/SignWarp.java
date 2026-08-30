package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;

import java.util.concurrent.CompletableFuture;

public class SignWarp extends EssentialsSign {
    public SignWarp() {
        super("Warp");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 3, ess);
        final String warpName = sign.getLine(1);
        if (warpName.isEmpty()) {
            sign.setLine(1, "\u00a7c<Warp name>");
            throw new SignException("invalidSignLine", 1);
        } else {
            try {
                ess.getWarps().getWarp(warpName);
            } catch (final Exception ex) {
                if (ex instanceof TranslatableException) {
                    final TranslatableException te = (TranslatableException) ex;
                    throw new SignException(ex, te.getTlKey(), te.getArgs());
                }
                throw new SignException(ex, "errorWithMessage", ex.getMessage());
            }
            final String group = sign.getLine(2);
            if ("Everyone".equalsIgnoreCase(group) || "Everybody".equalsIgnoreCase(group)) {
                sign.setLine(2, "\u00a72Everyone");
            }
            return true;
        }
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final String warpName = sign.getLine(1);
        final String group = sign.getLine(2);
        if (!group.isEmpty()) {
            if (!"\u00a72Everyone".equals(group) && !player.inGroup(group)) {
                throw new SignException("warpUsePermission");
            }
        } else {
            if (ess.getSettings().getPerWarpPermission() && !player.isAuthorized("essentials.warps." + warpName)) {
                throw new SignException("warpUsePermission");
            }
        }
        final Trade charge = getTrade(sign, 3, ess);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        player.getAsyncTeleport().warp(player, warpName, charge, TeleportCause.PLUGIN, future);
        future.thenAccept(success -> {
            if (success) {
                Trade.log("Sign", "Warp", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            }
        });
        future.exceptionally(e -> {
            ess.showError(player.getSource(), e, "\\ sign: " + signName);
            return false;
        });
        return true;
    }
}
