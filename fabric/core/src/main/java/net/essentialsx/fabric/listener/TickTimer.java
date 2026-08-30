package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

/**
 * Per-tick maintenance (upstream {@code EssentialsTimer}) driven by the server tick event:
 * TPS sampling, activity/AFK checks, mute/jail expiry, warmup progress, freeze/AFK movement
 * correction, god-mode upkeep and playtime accounting (Section 5.2).
 */
public class TickTimer {
    private final transient Essentials ess;
    private final transient Set<UUID> onlineUsers = new HashSet<>();
    private final LinkedList<Double> history = new LinkedList<>();
    private final long maxTime = 10 * 1000000;
    private final long tickInterval = 50;
    private transient long lastPoll = System.nanoTime();
    private int skip1 = 0;
    private int skip2 = 0;
    private long tickCounter = 0;

    public TickTimer(final Essentials ess) {
        this.ess = ess;
        history.add(20d);
    }

    /** Called at the end of every server tick. */
    public void tick() {
        tickCounter++;
        // Per-tick: warmups, freeze/AFK movement, god upkeep
        for (final User user : ess.getOnlineUsers()) {
            final ServerPlayer base = user.getBase();
            if (base == null) {
                continue;
            }
            user.getAsyncTeleport().tick();
            tickMovement(user, base);
            if (user.isGodModeEnabled()) {
                if (base.getRemainingFireTicks() > 0) {
                    base.setRemainingFireTicks(0);
                }
                if (base.getAirSupply() < base.getMaxAirSupply()) {
                    base.setAirSupply(base.getMaxAirSupply());
                }
                if (user.isGodModeEnabledRaw() && base.getFoodData().getFoodLevel() < 20) {
                    base.getFoodData().setFoodLevel(20);
                    base.getFoodData().setSaturation(10);
                }
            }
        }
        if (tickCounter % 20 == 0) {
            run();
        }
        if (tickCounter % 1200 == 0) {
            ess.getUsers().cleanupCache();
            if (ess.getEconomy() != null) {
                ess.getEconomy().cleanupCache();
            }
            final long serverTick = ess.getServer().getTickCount();
            for (final User user : ess.getOnlineUsers()) {
                user.flushPlaytime(serverTick);
            }
        }
    }

    private void tickMovement(final User user, final ServerPlayer base) {
        final LazyLocation current = LazyLocation.of(base);
        final LazyLocation last = user.getLastKnownPosition();
        user.setLastKnownPosition(current);
        if (last == null) {
            return;
        }
        if (last.sameBlock(current)) {
            return;
        }
        if (user.isFreeze()) {
            teleportBack(user, base, last, current);
            return;
        }
        if (!ess.getSettings().cancelAfkOnMove() && !ess.getSettings().getFreezeAfkPlayers()) {
            return;
        }
        if (user.isAfk() && ess.getSettings().getFreezeAfkPlayers()) {
            if (current.y() >= last.blockY() + 1) {
                user.updateActivityOnMove(true);
                return;
            }
            if (!base.getAbilities().mayfly) {
                teleportBack(user, base, last, current);
            } else {
                teleportBack(user, base, last, current);
            }
            return;
        }
        final LazyLocation afk = user.getAfkPosition();
        if (afk == null || !current.sameWorld(afk) || afk.distanceSquared(current) > 9) {
            user.updateActivityOnMove(true);
        }
    }

    private void teleportBack(final User user, final ServerPlayer base, final LazyLocation last, final LazyLocation current) {
        LazyLocation to = current.withPosition(last.x(), last.y(), last.z());
        final net.minecraft.server.level.ServerLevel level = to.level(ess.getServer());
        if (level == null) {
            return;
        }
        if (!base.getAbilities().mayfly) {
            try {
                to = LocationUtil.getSafeDestination(ess, to);
            } catch (final Exception ignored) {
            }
        }
        base.connection.teleport(to.x(), to.y(), to.z(), current.yaw(), current.pitch());
        user.setLastKnownPosition(to);
    }

    private void run() {
        final long startTime = System.nanoTime();
        final long currentTime = System.currentTimeMillis();
        long timeSpent = (startTime - lastPoll) / 1000;
        if (timeSpent == 0) {
            timeSpent = 1;
        }
        if (history.size() > 10) {
            history.remove();
        }
        final double tps = 20 * tickInterval * 1000000.0 / timeSpent;
        if (tps <= 21) {
            history.add(tps);
        }
        lastPoll = startTime;
        int count = 0;
        onlineUsers.clear();
        for (final ServerPlayer player : ess.getOnlinePlayers()) {
            count++;
            if (skip1 > 0) {
                skip1--;
                continue;
            }
            if (count % 10 == 0) {
                if (System.nanoTime() - startTime > maxTime / 2) {
                    skip1 = count - 1;
                    break;
                }
            }
            try {
                final User user = ess.getUser(player);
                onlineUsers.add(user.getUUID());
                user.setLastOnlineActivity(currentTime);
                user.checkActivity();
            } catch (final Exception e) {
                ess.getLogger().warn("EssentialsTimer Error:", e);
            }
        }
        count = 0;
        final Iterator<UUID> iterator = onlineUsers.iterator();
        while (iterator.hasNext()) {
            count++;
            if (skip2 > 0) {
                skip2--;
                continue;
            }
            if (count % 10 == 0) {
                if (System.nanoTime() - startTime > maxTime) {
                    skip2 = count - 1;
                    break;
                }
            }
            final User user = ess.getUser(iterator.next());
            if (user == null) {
                iterator.remove();
                continue;
            }
            if (user.getLastOnlineActivity() < currentTime && user.getLastOnlineActivity() > user.getLastLogout()) {
                if (!user.isHidden()) {
                    user.setLastLogout(user.getLastOnlineActivity());
                }
                iterator.remove();
                continue;
            }
            user.checkMuteTimeout(currentTime);
            user.checkJailTimeout(currentTime);
            user.resetInvulnerabilityAfterTeleport();
        }
    }

    public double getAverageTPS() {
        double avg = 0;
        for (final Double f : history) {
            if (f != null) {
                avg += f;
            }
        }
        return avg / history.size();
    }

    public boolean isGameModeAllowingFreeze(final ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
    }
}
