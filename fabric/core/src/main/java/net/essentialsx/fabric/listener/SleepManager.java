package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players that are ignored for the sleep percentage (AFK, vanished, permission).
 * Consulted by the {@code SleepStatus} mixin (Section 13.3).
 */
public class SleepManager {
    private final Essentials ess;
    private final Set<UUID> sleepingIgnored = ConcurrentHashMap.newKeySet();

    public SleepManager(final Essentials ess) {
        this.ess = ess;
    }

    public void update(final User user) {
        if (user.getBase() == null) {
            sleepingIgnored.remove(user.getUUID());
            return;
        }
        final boolean ignored = user.isAuthorized("essentials.sleepingignored")
            || (user.isAfk() && ess.getSettings().sleepIgnoresAfkPlayers())
            || (user.isVanished() && ess.getSettings().sleepIgnoresVanishedPlayers());
        if (ignored) {
            sleepingIgnored.add(user.getUUID());
        } else {
            sleepingIgnored.remove(user.getUUID());
        }
    }

    public void remove(final UUID uuid) {
        sleepingIgnored.remove(uuid);
    }

    public boolean isSleepingIgnored(final UUID uuid) {
        return sleepingIgnored.contains(uuid);
    }
}
