package net.essentialsx.fabric.teleport;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.WarpNotFoundException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Teleport kernel (Section 8). Every Essentials teleport passes through this class:
 * cooldown → warmup → revalidation → safety → atomic move → back-location capture.
 */
public class AsyncTeleport {
    private final IUser teleportOwner;
    private final Essentials ess;
    private AsyncTimedTeleport timedTeleport;
    private TeleportType tpType;

    public AsyncTeleport(final IUser user, final Essentials ess) {
        this.teleportOwner = user;
        this.ess = ess;
        tpType = TeleportType.NORMAL;
    }

    public void cooldown(final boolean check) throws Throwable {
        final CompletableFuture<Boolean> exceptionFuture = new CompletableFuture<>();
        if (cooldown(check, exceptionFuture)) {
            try {
                exceptionFuture.get();
            } catch (final ExecutionException e) {
                throw e.getCause();
            }
        }
    }

    public boolean cooldown(final boolean check, final CompletableFuture<Boolean> future) {
        final Calendar time = new GregorianCalendar();
        if (teleportOwner.getLastTeleportTimestamp() > 0) {
            final double cooldown = ess.getSettings().getTeleportCooldown();
            final Calendar earliestTime = new GregorianCalendar();
            earliestTime.add(Calendar.SECOND, -(int) cooldown);
            earliestTime.add(Calendar.MILLISECOND, -(int) ((cooldown * 1000.0) % 1000.0));
            final long earliestLong = earliestTime.getTimeInMillis();
            final long lastTime = teleportOwner.getLastTeleportTimestamp();
            if (lastTime > time.getTimeInMillis()) {
                teleportOwner.setLastTeleportTimestamp(time.getTimeInMillis());
                return false;
            } else if (lastTime > earliestLong && cooldownApplies()) {
                time.setTimeInMillis(lastTime);
                time.add(Calendar.SECOND, (int) cooldown);
                time.add(Calendar.MILLISECOND, (int) ((cooldown * 1000.0) % 1000.0));
                future.completeExceptionally(new TranslatableException("timeBeforeTeleport", DateUtil.formatDateDiff(time.getTimeInMillis())));
                return true;
            }
        }
        if (!check) {
            teleportOwner.setLastTeleportTimestamp(time.getTimeInMillis());
        }
        return false;
    }

    private boolean cooldownApplies() {
        boolean applies = true;
        final String globalBypassPerm = "essentials.teleport.cooldown.bypass";
        switch (tpType) {
            case NORMAL:
                applies = !teleportOwner.isAuthorized(globalBypassPerm);
                break;
            case BACK:
                applies = !(teleportOwner.isAuthorized(globalBypassPerm) && teleportOwner.isAuthorized("essentials.teleport.cooldown.bypass.back"));
                break;
            case TPA:
                applies = !(teleportOwner.isAuthorized(globalBypassPerm) && teleportOwner.isAuthorized("essentials.teleport.cooldown.bypass.tpa"));
                break;
        }
        return applies;
    }

    private void warnUser(final IUser user, final double delay) {
        final Calendar c = new GregorianCalendar();
        c.add(Calendar.SECOND, (int) delay);
        c.add(Calendar.MILLISECOND, (int) ((delay * 1000.0) % 1000.0));
        user.sendTl("dontMoveMessage", DateUtil.formatDateDiff(c.getTimeInMillis()));
    }

    public void now(final LazyLocation loc, final boolean cooldown, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        if (cooldown && cooldown(false, future)) {
            return;
        }
        final ITarget target = new LocationTarget(loc);
        nowAsync(teleportOwner, target, cause, future);
    }

    public void now(final ServerPlayer entity, final boolean cooldown, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        if (cooldown && cooldown(false, future)) {
            future.complete(false);
            return;
        }
        final ITarget target = new PlayerTarget(entity);
        nowAsync(teleportOwner, target, cause, future);
        future.thenAccept(success -> {
            if (success) {
                final LazyLocation loc = target.getLocation();
                teleportOwner.sendTl("teleporting", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
            }
        });
    }

    /**
     * Raw teleport without safety checks (used by administrative overrides).
     */
    public void nowUnsafe(final LazyLocation loc, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final ServerPlayer player = teleportOwner.getBase();
        if (player == null) {
            future.completeExceptionally(new TranslatableException("playerNotFound"));
            return;
        }
        final ServerLevel level = loc.level(ess.getServer());
        if (level == null) {
            future.completeExceptionally(new TranslatableException("invalidWorld"));
            return;
        }
        ess.scheduleSyncDelayedTask(() -> {
            performTeleport(player, level, loc);
            future.complete(true);
        });
    }

    /**
     * The single vanilla teleport call site of the mod (Section 8).
     */
    static void performTeleport(final ServerPlayer player, final ServerLevel level, final LazyLocation loc) {
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(loc.blockPos()), 1, player.getId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        player.teleportTo(level, loc.x(), loc.y(), loc.z(), Set.of(), loc.yaw(), loc.pitch());
        player.setYHeadRot(loc.yaw());
        player.fallDistance = 0;
    }

    protected void nowAsync(final IUser teleportee, final ITarget target, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        cancel(false);
        final ServerPlayer player = teleportee.getBase();
        if (player == null) {
            future.completeExceptionally(new TranslatableException("playerNotFound"));
            return;
        }
        if (!ess.getSettings().isForcePassengerTeleport() && !player.getPassengers().isEmpty()) {
            if (!ess.getSettings().isTeleportPassengerDismount()) {
                future.completeExceptionally(new TranslatableException("passengerTeleportFail"));
                return;
            }
            player.ejectPassengers();
        }
        final LazyLocation origin = LazyLocation.of(player);
        final LazyLocation targetLoc0 = target.getLocation();
        final ServerLevel level = targetLoc0.level(ess.getServer());
        if (level == null) {
            future.completeExceptionally(new TranslatableException("invalidWorld"));
            return;
        }
        if (!Double.isFinite(targetLoc0.x()) || !Double.isFinite(targetLoc0.y()) || !Double.isFinite(targetLoc0.z())) {
            future.completeExceptionally(new TranslatableException("invalidWorld"));
            return;
        }
        LazyLocation targetLoc = targetLoc0;
        if (ess.getSettings().isTeleportSafetyEnabled() && !ess.getSettings().isForceDisableTeleportSafety() && LocationUtil.isBlockOutsideWorldBorder(level, targetLoc.blockX(), targetLoc.blockZ())) {
            targetLoc = targetLoc.withPosition(LocationUtil.getXInsideWorldBorder(level, targetLoc.blockX()), targetLoc.y(), LocationUtil.getZInsideWorldBorder(level, targetLoc.blockZ()));
        }
        final LazyLocation finalTarget = targetLoc;
        final ChunkPos chunkPos = new ChunkPos(finalTarget.blockPos());
        // Load the destination chunk asynchronously, then finish on the server thread (Section 4.4).
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, player.getId());
        final CompletableFuture<?> chunkFuture = level.getChunkSource().getChunkFuture(chunkPos.x, chunkPos.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        chunkFuture.whenCompleteAsync((chunk, throwable) -> {
            try {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                if (!player.isAlive() || player.hasDisconnected()) {
                    future.complete(false);
                    return;
                }
                LazyLocation loc = finalTarget;
                if (LocationUtil.isBlockUnsafeForUser(ess, teleportee, level, loc.blockX(), loc.blockY(), loc.blockZ())) {
                    if (ess.getSettings().isTeleportSafetyEnabled()) {
                        if (!ess.getSettings().isForceDisableTeleportSafety()) {
                            try {
                                loc = LocationUtil.getSafeDestination(ess, teleportee, loc);
                            } catch (final Exception e) {
                                future.completeExceptionally(e);
                                return;
                            }
                        }
                    } else {
                        future.completeExceptionally(new TranslatableException("unsafeTeleportDestination", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ()));
                        return;
                    }
                } else if (!ess.getSettings().isForceDisableTeleportSafety() && ess.getSettings().isTeleportToCenterLocation()) {
                    loc = LocationUtil.getRoundedDestination(loc);
                }
                if (teleportee.isAuthorized("essentials.back.onteleport")) {
                    teleportee.setLastLocationIfChanged(origin);
                }
                performTeleport(player, level, loc);
                if (ess.getSettings().isTeleportInvulnerability()) {
                    teleportee.enableInvulnerabilityAfterTeleport();
                }
                future.complete(true);
            } catch (final Throwable t) {
                future.completeExceptionally(t);
            }
        }, ess.getServer());
    }

    public void teleport(final LazyLocation loc, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        teleport(teleportOwner, new LocationTarget(loc), chargeFor, cause, future);
    }

    public void teleport(final ServerPlayer entity, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        teleportOwner.sendTl("teleportToPlayer", ess.getUser(entity).getDisplayName());
        teleport(teleportOwner, new PlayerTarget(entity), chargeFor, cause, future);
    }

    public void teleportPlayer(final IUser otherUser, final LazyLocation loc, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        teleport(otherUser, new LocationTarget(loc), chargeFor, cause, future);
    }

    public void teleportPlayer(final IUser otherUser, final ServerPlayer entity, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final ITarget target = new PlayerTarget(entity);
        teleport(otherUser, target, chargeFor, cause, future);
        future.thenAccept(success -> {
            if (success) {
                final LazyLocation loc = target.getLocation();
                otherUser.sendTl("teleporting", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
                teleportOwner.sendTl("teleporting", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
            }
        });
    }

    private void teleport(final IUser teleportee, final ITarget target, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final double delay = ess.getSettings().getTeleportDelay();
        Trade cashCharge = chargeFor;
        if (chargeFor != null) {
            chargeFor.isAffordableFor(teleportOwner, future);
            if (future.isCompletedExceptionally()) {
                return;
            }
            if (!chargeFor.getCommandCost(teleportOwner).equals(BigDecimal.ZERO)) {
                cashCharge = new Trade(chargeFor.getCommandCost(teleportOwner), ess);
            }
        }
        if (cooldown(true, future)) {
            future.complete(false);
            return;
        }
        if (delay <= 0 || teleportOwner.isAuthorized("essentials.teleport.timer.bypass") || teleportee.isAuthorized("essentials.teleport.timer.bypass")) {
            if (cooldown(false, future)) {
                future.complete(false);
                return;
            }
            final Trade finalCharge = cashCharge;
            final CompletableFuture<Boolean> tpFuture = new CompletableFuture<>();
            tpFuture.whenComplete((success, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                if (success && finalCharge != null) {
                    finalCharge.charge(teleportOwner, future);
                    if (future.isCompletedExceptionally()) {
                        return;
                    }
                }
                future.complete(success);
            });
            nowAsync(teleportee, target, cause, tpFuture);
            return;
        }
        cancel(false);
        warnUser(teleportee, delay);
        initTimer((long) (delay * 1000.0), teleportee, target, cashCharge, cause, false, future);
    }

    private void teleportOther(final IUser teleporter, final IUser teleportee, final ITarget target, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final double delay = ess.getSettings().getTeleportDelay();
        Trade cashCharge = chargeFor;
        if (teleporter != null && chargeFor != null) {
            chargeFor.isAffordableFor(teleporter, future);
            if (future.isCompletedExceptionally()) {
                return;
            }
            if (!chargeFor.getCommandCost(teleporter).equals(BigDecimal.ZERO)) {
                cashCharge = new Trade(chargeFor.getCommandCost(teleporter), ess);
            }
        }
        if (cooldown(true, future)) {
            return;
        }
        if (delay <= 0 || teleporter == null
            || teleporter.isAuthorized("essentials.teleport.timer.bypass")
            || teleportOwner.isAuthorized("essentials.teleport.timer.bypass")
            || teleportee.isAuthorized("essentials.teleport.timer.bypass")) {
            if (cooldown(false, future)) {
                return;
            }
            final Trade finalCharge = cashCharge;
            final CompletableFuture<Boolean> tpFuture = new CompletableFuture<>();
            tpFuture.whenComplete((success, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                if (success && teleporter != null && finalCharge != null) {
                    finalCharge.charge(teleporter, future);
                    if (future.isCompletedExceptionally()) {
                        return;
                    }
                }
                future.complete(success);
            });
            nowAsync(teleportee, target, cause, tpFuture);
            return;
        }
        cancel(false);
        warnUser(teleportee, delay);
        initTimer((long) (delay * 1000.0), teleportee, target, cashCharge, cause, false, future);
    }

    public void respawn(final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final double delay = ess.getSettings().getTeleportDelay();
        if (chargeFor != null) {
            chargeFor.isAffordableFor(teleportOwner, future);
            if (future.isCompletedExceptionally()) {
                return;
            }
        }
        if (cooldown(true, future)) {
            return;
        }
        if (delay <= 0 || teleportOwner.isAuthorized("essentials.teleport.timer.bypass")) {
            if (cooldown(false, future)) {
                return;
            }
            final CompletableFuture<Boolean> tpFuture = new CompletableFuture<>();
            tpFuture.whenComplete((success, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                if (success && chargeFor != null) {
                    chargeFor.charge(teleportOwner, future);
                    if (future.isCompletedExceptionally()) {
                        return;
                    }
                }
                future.complete(success);
            });
            respawnNow(teleportOwner, cause, tpFuture);
            return;
        }
        cancel(false);
        warnUser(teleportOwner, delay);
        initTimer((long) (delay * 1000.0), teleportOwner, null, chargeFor, cause, true, future);
    }

    void respawnNow(final IUser teleportee, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final ServerPlayer player = teleportee.getBase();
        if (player == null) {
            future.complete(false);
            return;
        }
        final LazyLocation location = ess.getRespawnLocation(player);
        nowAsync(teleportee, new LocationTarget(location), cause, future);
    }

    public void warp(final IUser otherUser, final String warp, final Trade chargeFor, final TeleportCause cause, final CompletableFuture<Boolean> future) {
        final LazyLocation loc;
        try {
            loc = ess.getWarps().getWarp(warp);
        } catch (final WarpNotFoundException e) {
            future.completeExceptionally(e);
            return;
        }
        future.thenAccept(success -> {
            if (success) {
                otherUser.sendTl("warpingTo", warp, loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
                if (!otherUser.equals(teleportOwner)) {
                    teleportOwner.sendTl("warpingTo", warp, loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
                }
            }
        });
        teleport(otherUser, new LocationTarget(loc), chargeFor, cause, future);
    }

    public void back(final Trade chargeFor, final CompletableFuture<Boolean> future) {
        back(teleportOwner, chargeFor, future);
    }

    public void back(final IUser teleporter, final Trade chargeFor, final CompletableFuture<Boolean> future) {
        tpType = TeleportType.BACK;
        final LazyLocation loc = teleportOwner.getLastLocation();
        if (loc == null) {
            future.completeExceptionally(new TranslatableException("noLocationFound"));
            return;
        }
        teleportOwner.sendTl("backUsageMsg", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
        teleportOther(teleporter, teleportOwner, new LocationTarget(loc), chargeFor, TeleportCause.COMMAND, future);
    }

    public void back(final CompletableFuture<Boolean> future) {
        final LazyLocation loc = teleportOwner.getLastLocation();
        if (loc == null) {
            future.completeExceptionally(new TranslatableException("noLocationFound"));
            return;
        }
        nowAsync(teleportOwner, new LocationTarget(loc), TeleportCause.COMMAND, future);
    }

    public TeleportType getTpType() {
        return this.tpType;
    }

    public void setTpType(final TeleportType tpType) {
        this.tpType = tpType;
    }

    public boolean hasPendingTimer() {
        return timedTeleport != null;
    }

    public void cancel(final boolean notifyUser) {
        if (timedTeleport != null) {
            timedTeleport.cancelTimer(notifyUser);
            timedTeleport = null;
        }
    }

    private void initTimer(final long delay, final IUser teleportUser, final ITarget target, final Trade chargeFor, final TeleportCause cause, final boolean respawn, final CompletableFuture<Boolean> future) {
        timedTeleport = new AsyncTimedTeleport(teleportOwner, ess, this, delay, future, teleportUser, target, chargeFor, cause, respawn);
    }

    /** Called from the tick timer for warmup progress. */
    public void tick() {
        if (timedTeleport != null) {
            timedTeleport.tick();
        }
    }

    public enum TeleportType {
        TPA,
        BACK,
        NORMAL
    }
}
