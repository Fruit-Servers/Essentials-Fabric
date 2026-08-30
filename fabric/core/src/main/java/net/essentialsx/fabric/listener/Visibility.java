package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanish visibility graph (Section 13.2). Decides, per observer/subject pair, whether the
 * subject is visible. Entity tracking and tab-list packets are filtered by mixins that
 * consult {@link #canSee(ServerPlayer, ServerPlayer)}.
 */
public class Visibility {
    private final Essentials ess;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public Visibility(final Essentials ess) {
        this.ess = ess;
    }

    public boolean isVanished(final UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean isVanished(final ServerPlayer player) {
        return player != null && vanished.contains(player.getUUID());
    }

    public Set<UUID> getVanished() {
        return vanished;
    }

    /**
     * Whether {@code observer} may see {@code subject}.
     */
    public boolean canSee(final ServerPlayer observer, final ServerPlayer subject) {
        if (observer == null || subject == null) {
            return true;
        }
        if (observer == subject || observer.getUUID().equals(subject.getUUID())) {
            return true;
        }
        if (!vanished.contains(subject.getUUID())) {
            return true;
        }
        final User observerUser = ess.getUser(observer);
        return observerUser != null && observerUser.isAuthorizedCached("essentials.vanish.see");
    }

    public boolean canSee(final UUID observer, final UUID subject) {
        if (observer.equals(subject) || !vanished.contains(subject)) {
            return true;
        }
        final ServerPlayer observerPlayer = ess.getServer().getPlayerList().getPlayer(observer);
        if (observerPlayer == null) {
            return false;
        }
        final User observerUser = ess.getUser(observerPlayer);
        return observerUser != null && observerUser.isAuthorizedCached("essentials.vanish.see");
    }

    /**
     * Called when a user's vanish state changes; resynchronises all observers.
     */
    public void onVanishChanged(final User user, final boolean nowVanished) {
        final ServerPlayer subject = user.getBase();
        if (subject == null) {
            if (nowVanished) {
                vanished.add(user.getUUID());
            } else {
                vanished.remove(user.getUUID());
            }
            return;
        }
        if (nowVanished) {
            vanished.add(subject.getUUID());
        } else {
            vanished.remove(subject.getUUID());
        }
        resync(subject);
    }

    /**
     * Re-evaluate visibility of the subject for every online observer (entity tracking + tab
     * list). Safe to call repeatedly; batched per observer to avoid packet spikes.
     */
    public void resync(final ServerPlayer subject) {
        final ServerLevel level = (ServerLevel) subject.level();
        for (final ServerPlayer observer : ess.getOnlinePlayers()) {
            if (observer == subject) {
                continue;
            }
            final boolean visible = canSee(observer, subject);
            if (visible) {
                observer.connection.send(new ClientboundPlayerInfoUpdatePacket(java.util.EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(subject)));
            } else {
                observer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(subject.getUUID())));
            }
        }
        // Force the entity tracker to re-evaluate: temporarily untrack and re-add the subject.
        level.getChunkSource().removeEntity(subject);
        level.getChunkSource().addEntity(subject);
    }

    /**
     * When a new observer joins, hide every vanished player they may not see.
     */
    public void onObserverJoin(final ServerPlayer observer) {
        for (final UUID uuid : vanished) {
            final ServerPlayer subject = ess.getServer().getPlayerList().getPlayer(uuid);
            if (subject != null && !canSee(observer, subject)) {
                observer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
            }
        }
    }

    public void remove(final UUID uuid) {
        vanished.remove(uuid);
    }
}
