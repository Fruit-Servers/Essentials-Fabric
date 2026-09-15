package net.essentialsx.fabric.perm;

import me.lucko.fabric.api.permissions.v0.Options;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.utils.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Permission and group/meta adapter (Section 7).
 *
 * <p>Resolution order: explicit provider result (fabric-permissions-api, backed by
 * LuckPerms or another provider) → operator/default policy → deny. Without a provider,
 * operators receive every node and ordinary players only the configured
 * {@code player-commands}.
 */
public class PermissionsHandler {
    private final Essentials ess;
    private final boolean luckPerms;
    private final Map<UUID, Map<String, Boolean>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTime = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.SECONDS.toMillis(5);
    /**
     * Nodes (without the {@code essentials.} prefix) that upstream plugin.yml declares
     * {@code default: true}: granted to everyone unless a provider says otherwise.
     */
    private static final Set<String> DEFAULT_TRUE = Set.of(
        "back.onteleport",
        "teleport.cooldown.bypass.tpa",
        "teleport.cooldown.bypass.back"
    );
    /**
     * Nodes (without the {@code essentials.} prefix) that upstream plugin.yml declares
     * {@code default: false}: never implied, not even for operators. Exemptions, bypasses and
     * opt-in behaviour need an explicit provider grant or a {@code player-commands} entry.
     */
    private static final Set<String> DEFAULT_FALSE = Set.of(
        "afk.auto",
        "back.ondeath",
        "balancetop.exclude",
        "ban.exempt",
        "chat.ignoreexempt",
        "exempt",
        "home.compass",
        "invsee.preventmodify",
        "keepinv",
        "keepxp",
        "kick.exempt",
        "kit.exemptdelay",
        "mute.exempt",
        "near.exclude",
        "nick.allowunsafe",
        "nick.hideprefix",
        "sethome.bed",
        "silentjoin",
        "silentjoin.vanish",
        "silentquit",
        "spawn-on-join.exempt",
        "sudo.exempt",
        "tempban.exempt"
    );

    public PermissionsHandler(final Essentials ess) {
        this.ess = ess;
        this.luckPerms = FabricLoader.getInstance().isModLoaded("luckperms");
    }

    public String getName() {
        if (luckPerms) {
            return "LuckPerms";
        }
        return "fabric-permissions-api";
    }

    public boolean hasLuckPerms() {
        return luckPerms;
    }

    private boolean isOp(final ServerPlayer player) {
        return ess.getServer().getPlayerList().isOp(player.getGameProfile());
    }

    /**
     * Default policy when no provider answers (Section 7.1).
     */
    private boolean defaultPolicy(final ServerPlayer player, final String node) {
        if (!node.startsWith("essentials.")) {
            return isOp(player);
        }
        final String rest = node.substring("essentials.".length());
        // Upstream ConfigPermissionsHandler grants only an exact player-commands entry:
        // "kit" grants essentials.kit but never essentials.kit.exemptdelay, so kit
        // cooldowns, "others" variants and other sub nodes stay opt-in.
        if (ess.getSettings().isPlayerCommand(rest)) {
            return true;
        }
        if (DEFAULT_FALSE.contains(rest)) {
            return false;
        }
        return isOp(player) || DEFAULT_TRUE.contains(rest);
    }

    public boolean hasPermission(final ServerPlayer player, final String node) {
        final long start = System.nanoTime();
        try {
            final TriState state = TriState.of(Permissions.getPermissionValue(player, node));
            if (state != TriState.UNSET) {
                return state == TriState.TRUE;
            }
            // Wildcard support for providers that do not implement it
            final TriState wildcard = checkWildcards(player, node);
            if (wildcard != TriState.UNSET) {
                return wildcard == TriState.TRUE;
            }
            return defaultPolicy(player, node);
        } finally {
            final long elapsed = System.nanoTime() - start;
            if (elapsed > ess.getSettings().getPermissionsLagWarning()) {
                ess.getLogger().info("Lag Notice - Slow Permission System Response - Request took over {}ms!", elapsed / 1000000.0);
            }
        }
    }

    private TriState checkWildcards(final ServerPlayer player, final String node) {
        String current = node;
        while (true) {
            final int dot = current.lastIndexOf('.');
            if (dot == -1) {
                break;
            }
            current = current.substring(0, dot);
            final TriState state = TriState.of(Permissions.getPermissionValue(player, current + ".*"));
            if (state != TriState.UNSET) {
                return state;
            }
        }
        return TriState.of(Permissions.getPermissionValue(player, "*"));
    }

    public boolean hasPermissionCached(final ServerPlayer player, final String node) {
        final UUID uuid = player.getUUID();
        final Long time = cacheTime.get(uuid);
        if (time == null || System.currentTimeMillis() - time > CACHE_TTL) {
            cache.remove(uuid);
            cacheTime.put(uuid, System.currentTimeMillis());
        }
        return cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).computeIfAbsent(node, n -> hasPermission(player, n));
    }

    public void invalidatePermissionCache(final UUID uuid) {
        cache.remove(uuid);
        cacheTime.remove(uuid);
    }

    public boolean isPermissionSet(final ServerPlayer player, final String node) {
        return Permissions.getPermissionValue(player, node) != net.fabricmc.fabric.api.util.TriState.DEFAULT;
    }

    public TriState isPermissionSetExact(final ServerPlayer player, final String node) {
        return TriState.of(Permissions.getPermissionValue(player, node));
    }

    /**
     * Offline permission check (login gates). Providers such as LuckPerms answer through the
     * offline event; without a provider only operators are granted.
     */
    public CompletableFuture<Boolean> isOfflinePermissionSet(final UUID uuid, final String node) {
        return Permissions.getPermissionValue(uuid, node).thenApply(state -> {
            if (state != net.fabricmc.fabric.api.util.TriState.DEFAULT) {
                return state == net.fabricmc.fabric.api.util.TriState.TRUE;
            }
            final com.mojang.authlib.GameProfile profile = ess.getServer().getProfileCache() == null ? null : ess.getServer().getProfileCache().get(uuid).orElse(null);
            return profile != null && ess.getServer().getPlayerList().isOp(profile);
        });
    }

    public String getGroup(final ServerPlayer player) {
        final String group = Options.get(player, "primarygroup").orElse(null);
        if (group != null && !group.isEmpty()) {
            return group;
        }
        if (luckPerms) {
            final String lp = LuckPermsBridge.getPrimaryGroup(player.getUUID());
            if (lp != null) {
                return lp;
            }
        }
        return "default";
    }

    public List<String> getGroups(final ServerPlayer player) {
        if (luckPerms) {
            final List<String> groups = LuckPermsBridge.getGroups(player.getUUID());
            if (groups != null) {
                return groups;
            }
        }
        final List<String> groups = new ArrayList<>();
        groups.add(getGroup(player));
        return groups;
    }

    public boolean inGroup(final ServerPlayer player, final String group) {
        if (getGroup(player).equalsIgnoreCase(group)) {
            return true;
        }
        final TriState state = TriState.of(Permissions.getPermissionValue(player, "group." + group.toLowerCase(Locale.ENGLISH)));
        if (state != TriState.UNSET) {
            return state == TriState.TRUE;
        }
        if (luckPerms) {
            final List<String> groups = LuckPermsBridge.getGroups(player.getUUID());
            if (groups != null) {
                for (final String g : groups) {
                    if (g.equalsIgnoreCase(group)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getPrefix(final ServerPlayer player) {
        return Options.get(player, "prefix").orElse("");
    }

    public String getSuffix(final ServerPlayer player) {
        return Options.get(player, "suffix").orElse("");
    }

    public String getMeta(final ServerPlayer player, final String key) {
        return Options.get(player, key).orElse(null);
    }

    public boolean canBuild(final ServerPlayer player, final String group) {
        return true;
    }

    public List<String> getGroupNames() {
        if (luckPerms) {
            final List<String> groups = LuckPermsBridge.getAllGroups();
            if (groups != null) {
                return groups;
            }
        }
        return Collections.singletonList("default");
    }

    public void unregisterContexts() {
        cache.clear();
        cacheTime.clear();
    }
}
