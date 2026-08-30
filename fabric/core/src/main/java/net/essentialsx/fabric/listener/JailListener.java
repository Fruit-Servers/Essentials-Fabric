package net.essentialsx.fabric.listener;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Jail enforcement (Section 9.3): join return, teleport override, game-mode lock, respawn.
 * Block/interact restrictions are enforced in {@link BlockListener}.
 */
public final class JailListener {
    private static final Set<UUID> allowedTeleports = ConcurrentHashMap.newKeySet();

    private JailListener() {
    }

    /** Permit the next Essentials-initiated teleport for a jailed user (send-to-jail). */
    public static void allowTeleport(final UUID uuid) {
        allowedTeleports.add(uuid);
    }

    public static boolean consumeAllowedTeleport(final UUID uuid) {
        return allowedTeleports.remove(uuid);
    }

    public static void onJoin(final Essentials ess, final User user) {
        final long currentTime = System.currentTimeMillis();
        user.checkJailTimeout(currentTime);
        if (!user.isJailed() || user.getJail() == null || user.getJail().isEmpty()) {
            return;
        }
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.exceptionally(ex -> {
            ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage())));
            return false;
        });
        future.thenAccept(success -> user.sendTl("jailMessage"));
        try {
            ess.getJails().sendToJail(user, user.getJail(), future);
        } catch (final Exception ex) {
            future.completeExceptionally(ex);
        }
    }

    /**
     * Teleport policy for jailed players: any teleport that is not the jail send is redirected
     * to the jail (consulted from the teleport mixin). Returns the override destination or null.
     */
    public static LazyLocation overrideTeleport(final Essentials ess, final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null || !user.isJailed() || user.getJail() == null || user.getJail().isEmpty()) {
            return null;
        }
        if (consumeAllowedTeleport(player.getUUID())) {
            return null;
        }
        try {
            final LazyLocation jail = ess.getJails().getJail(user.getJail());
            user.sendTl("jailMessage");
            return jail;
        } catch (final Exception ex) {
            ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage())));
            return null;
        }
    }

    /** Respawn destination for jailed players. */
    public static LazyLocation respawnLocation(final Essentials ess, final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        if (user == null || !user.isJailed() || user.getJail() == null || user.getJail().isEmpty()) {
            return null;
        }
        try {
            return ess.getJails().getJail(user.getJail());
        } catch (final Exception ex) {
            ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage())));
            return null;
        }
    }

    /** Game-mode changes are blocked for jailed players. */
    public static boolean canChangeGameMode(final Essentials ess, final ServerPlayer player) {
        final User user = ess.getUsers().getOnlineUserCache().get(player.getUUID());
        return user == null || !user.isJailed();
    }
}
