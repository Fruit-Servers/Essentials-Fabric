package net.essentialsx.fabric.user;

import com.mojang.authlib.GameProfile;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.AsyncWriter;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User identity and cache (Section 6.2): UUID is canonical, names are a case-folded index
 * stored in {@code usermap.csv} ({@code name,uuid} lines, compatible with EssentialsX).
 */
public class UserMap {
    private final Essentials ess;
    private final Path userdataFolder;
    private final Path usermapFile;
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    private final Map<UUID, User> onlineUsers = new ConcurrentHashMap<>();
    private final Map<UUID, User> offlineCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> offlineCacheTime = new ConcurrentHashMap<>();
    private final Set<UUID> knownUuids = ConcurrentHashMap.newKeySet();

    public UserMap(final Essentials ess) {
        this.ess = ess;
        this.userdataFolder = ess.getDataFolder().resolve("userdata");
        this.usermapFile = ess.getDataFolder().resolve("usermap.csv");
        load();
    }

    private void load() {
        try {
            Files.createDirectories(userdataFolder);
        } catch (final IOException ignored) {
        }
        nameToUuid.clear();
        uuidToName.clear();
        knownUuids.clear();
        if (Files.exists(usermapFile)) {
            try {
                for (final String line : Files.readAllLines(usermapFile, StandardCharsets.UTF_8)) {
                    final String[] parts = line.split(",", 2);
                    if (parts.length != 2) {
                        continue;
                    }
                    final UUID uuid = StringUtil.toUUID(parts[1].trim());
                    if (uuid == null) {
                        continue;
                    }
                    final String name = parts[0].trim();
                    nameToUuid.put(name.toLowerCase(Locale.ENGLISH), uuid);
                    uuidToName.put(uuid, name);
                    knownUuids.add(uuid);
                }
            } catch (final IOException e) {
                ess.getLogger().error("Failed to read usermap.csv", e);
            }
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userdataFolder, "*.yml")) {
            for (final Path path : stream) {
                final String fileName = path.getFileName().toString();
                final UUID uuid = StringUtil.toUUID(fileName.substring(0, fileName.length() - 4));
                if (uuid != null) {
                    knownUuids.add(uuid);
                }
            }
        } catch (final IOException ignored) {
        }
    }

    private void saveUsermap() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<UUID, String> entry : uuidToName.entrySet()) {
            sb.append(entry.getValue()).append(',').append(entry.getKey()).append('\n');
        }
        AsyncWriter.write(usermapFile, sb.toString());
    }

    public int getUserCount() {
        return knownUuids.size();
    }

    public Set<UUID> getAllUserUUIDs() {
        return Collections.unmodifiableSet(knownUuids);
    }

    public Map<String, UUID> getNameCache() {
        return Collections.unmodifiableMap(nameToUuid);
    }

    public boolean userExists(final UUID uuid) {
        return knownUuids.contains(uuid);
    }

    public Collection<User> getOnlineUsers() {
        return onlineUsers.values();
    }

    public Map<UUID, User> getOnlineUserCache() {
        return onlineUsers;
    }

    public String getName(final UUID uuid) {
        return uuidToName.get(uuid);
    }

    public UUID getUuid(final String name) {
        return nameToUuid.get(name.toLowerCase(Locale.ENGLISH));
    }

    // ---------------------------------------------------------------- lookups

    public User getUser(final ServerPlayer player) {
        if (player == null) {
            return null;
        }
        final User existing = onlineUsers.get(player.getUUID());
        if (existing != null) {
            if (existing.getBase() != player) {
                existing.update(player);
            }
            return existing;
        }
        final User cached = offlineCache.remove(player.getUUID());
        offlineCacheTime.remove(player.getUUID());
        final User user;
        if (cached != null) {
            cached.update(player);
            user = cached;
        } else {
            user = new User(player, ess);
        }
        onlineUsers.put(player.getUUID(), user);
        trackName(player.getUUID(), player.getGameProfile().getName());
        return user;
    }

    public User getUser(final UUID uuid) {
        if (uuid == null) {
            return null;
        }
        final User online = onlineUsers.get(uuid);
        if (online != null) {
            return online;
        }
        final ServerPlayer player = ess.getServer() == null ? null : ess.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            return getUser(player);
        }
        final User cached = offlineCache.get(uuid);
        if (cached != null) {
            offlineCacheTime.put(uuid, System.currentTimeMillis());
            return cached;
        }
        if (!knownUuids.contains(uuid)) {
            return null;
        }
        final User user = new User(uuid, uuidToName.get(uuid), ess);
        offlineCache.put(uuid, user);
        offlineCacheTime.put(uuid, System.currentTimeMillis());
        return user;
    }

    /**
     * Case-insensitive name lookup, online first then the name index.
     */
    public User getUser(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (ess.getServer() != null) {
            final ServerPlayer player = ess.getServer().getPlayerList().getPlayerByName(name);
            if (player != null) {
                return getUser(player);
            }
        }
        final UUID uuid = nameToUuid.get(name.toLowerCase(Locale.ENGLISH));
        if (uuid != null) {
            return getUser(uuid);
        }
        // Fallback: offline profile cache (no network)
        if (ess.getServer() != null && ess.getServer().getProfileCache() != null) {
            final Optional<GameProfile> profile = ess.getServer().getProfileCache().get(name);
            if (profile.isPresent() && knownUuids.contains(profile.get().getId())) {
                return getUser(profile.get().getId());
            }
        }
        return null;
    }

    /**
     * Creates (or loads) the user record for a player joining for the first time.
     */
    public User loadOrCreate(final UUID uuid, final String name) {
        final User existing = getUser(uuid);
        if (existing != null) {
            return existing;
        }
        knownUuids.add(uuid);
        trackName(uuid, name);
        final User user = new User(uuid, name, ess);
        offlineCache.put(uuid, user);
        offlineCacheTime.put(uuid, System.currentTimeMillis());
        return user;
    }

    public void trackName(final UUID uuid, final String name) {
        if (name == null) {
            return;
        }
        knownUuids.add(uuid);
        final String lower = name.toLowerCase(Locale.ENGLISH);
        final String previous = uuidToName.put(uuid, name);
        if (previous != null && !previous.equalsIgnoreCase(name)) {
            nameToUuid.remove(previous.toLowerCase(Locale.ENGLISH), uuid);
        }
        final UUID old = nameToUuid.put(lower, uuid);
        if (previous == null || !previous.equals(name) || old == null || !old.equals(uuid)) {
            saveUsermap();
        }
    }

    public void invalidate(final UUID uuid) {
        onlineUsers.remove(uuid);
        offlineCache.remove(uuid);
        offlineCacheTime.remove(uuid);
        knownUuids.remove(uuid);
        final String name = uuidToName.remove(uuid);
        if (name != null) {
            nameToUuid.remove(name.toLowerCase(Locale.ENGLISH));
        }
        saveUsermap();
    }

    public void removeCache(final UUID uuid) {
        onlineUsers.remove(uuid);
        offlineCache.remove(uuid);
        offlineCacheTime.remove(uuid);
    }

    public void onQuit(final UUID uuid) {
        final User user = onlineUsers.remove(uuid);
        if (user != null) {
            offlineCache.put(uuid, user);
            offlineCacheTime.put(uuid, System.currentTimeMillis());
        }
    }

    /**
     * Evict idle offline users (called from the tick timer).
     */
    public void cleanupCache() {
        final long expiry = ess.getSettings().getMaxUserCacheValueExpiry() * 1000;
        final long now = System.currentTimeMillis();
        final List<UUID> expired = new ArrayList<>();
        for (final Map.Entry<UUID, Long> e : offlineCacheTime.entrySet()) {
            if (now - e.getValue() > expiry) {
                expired.add(e.getKey());
            }
        }
        for (final UUID uuid : expired) {
            final User user = offlineCache.remove(uuid);
            offlineCacheTime.remove(uuid);
            if (user != null) {
                user.save();
            }
        }
        final int max = ess.getSettings().getMaxUserCacheCount();
        if (offlineCache.size() > max) {
            offlineCache.entrySet().stream()
                .sorted((a, b) -> Long.compare(offlineCacheTime.getOrDefault(a.getKey(), 0L), offlineCacheTime.getOrDefault(b.getKey(), 0L)))
                .limit(offlineCache.size() - max)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(uuid -> {
                    offlineCache.remove(uuid);
                    offlineCacheTime.remove(uuid);
                });
        }
    }

    public CompletableFuture<Void> shutdown() {
        for (final User user : onlineUsers.values()) {
            user.getConfig().blockingSave();
        }
        for (final User user : offlineCache.values()) {
            user.getConfig().blockingSave();
        }
        onlineUsers.clear();
        offlineCache.clear();
        offlineCacheTime.clear();
        return CompletableFuture.completedFuture(null);
    }

    public Path getUserdataFolder() {
        return userdataFolder;
    }
}
