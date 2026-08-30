package net.essentialsx.fabric.teleport;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.user.IUser;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Teleport warmup (Section 8.3). Driven from the server tick rather than wall-clock timers;
 * cancelled on movement, damage, logout or supersession.
 */
public class AsyncTimedTeleport {
    private static final double MOVE_CONSTANT = 0.3;
    private final IUser teleportOwner;
    private final Essentials ess;
    private final AsyncTeleport teleport;
    private final UUID timer_teleportee;
    private final long timer_started;
    private final long timer_delay;
    private final CompletableFuture<Boolean> parentFuture;
    private final long timer_initX;
    private final long timer_initY;
    private final long timer_initZ;
    private final ITarget timer_teleportTarget;
    private final boolean timer_respawn;
    private final boolean timer_canMove;
    private final Trade timer_chargeFor;
    private final TeleportCause timer_cause;
    private boolean active = true;
    private int tickCounter = 0;
    private double timer_health;

    AsyncTimedTeleport(final IUser user, final Essentials ess, final AsyncTeleport teleport, final long delay, final CompletableFuture<Boolean> future, final IUser teleportUser, final ITarget target, final Trade chargeFor, final TeleportCause cause, final boolean respawn) {
        this.teleportOwner = user;
        this.ess = ess;
        this.teleport = teleport;
        this.timer_started = System.currentTimeMillis();
        this.timer_delay = delay;
        final ServerPlayer base = teleportUser.getBase();
        this.timer_health = base == null ? 0 : base.getHealth();
        this.timer_initX = base == null ? 0 : Math.round(base.getX() * MOVE_CONSTANT);
        this.timer_initY = base == null ? 0 : Math.round(base.getY() * MOVE_CONSTANT);
        this.timer_initZ = base == null ? 0 : Math.round(base.getZ() * MOVE_CONSTANT);
        this.timer_teleportee = teleportUser.getUUID();
        this.timer_teleportTarget = target;
        this.timer_chargeFor = chargeFor;
        this.timer_cause = cause;
        this.timer_respawn = respawn;
        this.timer_canMove = user.isAuthorized("essentials.teleport.timer.move");
        if (future != null) {
            this.parentFuture = future;
            return;
        }
        final CompletableFuture<Boolean> cFuture = new CompletableFuture<>();
        cFuture.exceptionally(e -> {
            ess.showError(teleportOwner.getSource(), e, "\\ teleport");
            return false;
        });
        this.parentFuture = cFuture;
    }

    /**
     * Called every server tick; checks once per second like upstream.
     */
    void tick() {
        if (!active) {
            return;
        }
        if (++tickCounter % 20 != 0) {
            return;
        }
        run();
    }

    private void run() {
        if (teleportOwner == null || !teleportOwner.isOnline()) {
            cancelTimer(false);
            return;
        }
        final IUser teleportUser = ess.getUser(this.timer_teleportee);
        if (teleportUser == null || !teleportUser.isOnline()) {
            cancelTimer(false);
            return;
        }
        final ServerPlayer base = teleportUser.getBase();
        if (!timer_canMove && (Math.round(base.getX() * MOVE_CONSTANT) != timer_initX || Math.round(base.getY() * MOVE_CONSTANT) != timer_initY || Math.round(base.getZ() * MOVE_CONSTANT) != timer_initZ || base.getHealth() < timer_health)) {
            cancelTimer(true);
            return;
        }
        timer_health = base.getHealth();
        final long now = System.currentTimeMillis();
        if (now > timer_started + timer_delay) {
            try {
                teleport.cooldown(false);
            } catch (final Throwable ex) {
                teleportOwner.sendTl("cooldownWithMessage", ex.getMessage());
                if (teleportOwner != teleportUser) {
                    teleportUser.sendTl("cooldownWithMessage", ex.getMessage());
                }
            }
            try {
                cancelTimer(false);
                teleportUser.sendTl("teleportationCommencing");
                if (timer_chargeFor != null) {
                    timer_chargeFor.isAffordableFor(teleportOwner);
                }
                final CompletableFuture<Boolean> tpFuture = new CompletableFuture<>();
                tpFuture.whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        parentFuture.completeExceptionally(throwable);
                        return;
                    }
                    if (success && timer_chargeFor != null) {
                        try {
                            timer_chargeFor.charge(teleportOwner);
                        } catch (final ChargeException ex) {
                            ess.showError(teleportOwner.getSource(), ex, "\\ teleport");
                        }
                    }
                    parentFuture.complete(success);
                });
                if (timer_respawn) {
                    teleport.respawnNow(teleportUser, timer_cause, tpFuture);
                } else {
                    teleport.nowAsync(teleportUser, timer_teleportTarget, timer_cause, tpFuture);
                }
            } catch (final Exception ex) {
                ess.showError(teleportOwner.getSource(), ex, "\\ teleport");
            }
        }
    }

    void cancelTimer(final boolean notifyUser) {
        if (!active) {
            return;
        }
        active = false;
        if (notifyUser) {
            teleportOwner.sendTl("pendingTeleportCancelled");
            if (timer_teleportee != null && !timer_teleportee.equals(teleportOwner.getUUID())) {
                final IUser other = ess.getUser(timer_teleportee);
                if (other != null) {
                    other.sendTl("pendingTeleportCancelled");
                }
            }
            parentFuture.complete(false);
        }
    }
}
