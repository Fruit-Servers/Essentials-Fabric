package net.essentialsx.fabric.spawn;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Group-aware named spawn storage in {@code spawn.yml} (Section 15.1).
 */
public class SpawnStorage {
    private final Essentials ess;
    private final YamlFile config;
    private final Map<String, LazyLocation> spawns = new HashMap<>();

    SpawnStorage(final Essentials ess) {
        this.ess = ess;
        this.config = new YamlFile(ess.getDataFolder().resolve("spawn.yml"));
        reloadConfig();
    }

    @SuppressWarnings("unchecked")
    public void reloadConfig() {
        synchronized (spawns) {
            config.load();
            spawns.clear();
            final Map<String, Object> section = config.getSection("spawns");
            if (section != null) {
                for (final Map.Entry<String, Object> entry : section.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        final LazyLocation loc = LazyLocation.fromMap((Map<String, Object>) entry.getValue());
                        if (loc != null) {
                            spawns.put(entry.getKey().toLowerCase(Locale.ENGLISH), loc);
                        }
                    }
                }
            }
        }
    }

    public void setSpawn(final LazyLocation loc, String group) {
        group = group.toLowerCase(Locale.ENGLISH);
        synchronized (spawns) {
            spawns.put(group, loc);
            config.setProperty("spawns." + group, loc.toMap());
            config.save();
        }
    }

    /**
     * Group spawn lookup chain (Section 15.1): group → default → first overworld-like
     * dimension spawn → first dimension spawn.
     */
    public LazyLocation getSpawn(String group) {
        if (group == null) {
            return getWorldSpawn();
        }
        group = group.toLowerCase(Locale.ENGLISH);
        synchronized (spawns) {
            LazyLocation loc = spawns.get(group);
            if (loc == null) {
                loc = spawns.get("default");
            }
            if (loc != null) {
                if (loc.isAvailable(ess.getServer())) {
                    return loc;
                }
                ess.getLogger().warn("Configured spawn for group '{}' points to an unavailable dimension '{}'; using world spawn.", group, loc.world());
            }
            return getWorldSpawn();
        }
    }

    public boolean hasSpawn(final String group) {
        synchronized (spawns) {
            return spawns.containsKey(group.toLowerCase(Locale.ENGLISH));
        }
    }

    private LazyLocation getWorldSpawn() {
        for (final ServerLevel level : ess.getServer().getAllLevels()) {
            if (!Worlds.isOverworldLike(level)) {
                continue;
            }
            return ess.getWorldSpawn(level);
        }
        final ServerLevel first = ess.getServer().getAllLevels().iterator().next();
        return ess.getWorldSpawn(first);
    }
}
