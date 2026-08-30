package net.essentialsx.fabric.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dimension naming and lookup. Vanilla dimensions are referred to by their key path
 * ({@code overworld}, {@code the_nether}, {@code the_end}); modded dimensions by
 * {@code namespace:path}. Legacy Bukkit world names are accepted as aliases for migration.
 */
public final class Worlds {
    private static final Map<String, String> LEGACY_ALIASES = Map.of(
        "world", "minecraft:overworld",
        "world_nether", "minecraft:the_nether",
        "world_the_end", "minecraft:the_end",
        "nether", "minecraft:the_nether",
        "end", "minecraft:the_end",
        "normal", "minecraft:overworld"
    );
    private static Map<String, String> configuredAliases = Map.of();

    private Worlds() {
    }

    public static void setConfiguredAliases(final Map<String, String> aliases) {
        configuredAliases = aliases == null ? Map.of() : aliases;
    }

    /** Full dimension key, e.g. {@code minecraft:overworld}. */
    public static String key(final ServerLevel level) {
        return level.dimension().location().toString();
    }

    /** Display/permission name: {@code overworld} for vanilla, {@code ns:path} otherwise. */
    public static String name(final ServerLevel level) {
        final ResourceLocation loc = level.dimension().location();
        if (loc.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return loc.getPath();
        }
        return loc.toString();
    }

    /** Name used for permission nodes: colons replaced. */
    public static String permissionName(final ServerLevel level) {
        return name(level).replace(':', '_');
    }

    public static ServerLevel get(final MinecraftServer server, final String name) {
        if (server == null || name == null || name.isEmpty()) {
            return null;
        }
        String lookup = name.trim();
        final String lower = lookup.toLowerCase(Locale.ENGLISH);
        if (configuredAliases.containsKey(lower)) {
            lookup = configuredAliases.get(lower);
        } else if (LEGACY_ALIASES.containsKey(lower)) {
            lookup = LEGACY_ALIASES.get(lower);
        }
        final ResourceLocation rl = ResourceLocation.tryParse(lookup.toLowerCase(Locale.ENGLISH));
        if (rl != null) {
            final ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
            if (level != null) {
                return level;
            }
        }
        // Case-insensitive match on name() and key()
        for (final ServerLevel level : server.getAllLevels()) {
            if (name(level).equalsIgnoreCase(lookup) || key(level).equalsIgnoreCase(lookup) || permissionName(level).equalsIgnoreCase(lookup)) {
                return level;
            }
        }
        return null;
    }

    public static ServerLevel getOrDefault(final MinecraftServer server, final String name, final ServerLevel def) {
        final ServerLevel level = get(server, name);
        return level == null ? def : level;
    }

    public static List<String> names(final MinecraftServer server) {
        final List<String> names = new ArrayList<>();
        for (final ServerLevel level : server.getAllLevels()) {
            names.add(name(level));
        }
        return names;
    }

    public static List<ServerLevel> all(final MinecraftServer server) {
        final List<ServerLevel> levels = new ArrayList<>();
        for (final ServerLevel level : server.getAllLevels()) {
            levels.add(level);
        }
        return levels;
    }

    public static boolean isOverworldLike(final ServerLevel level) {
        return level.dimensionType().natural() && !level.dimensionType().hasCeiling();
    }

    public static boolean isNether(final ServerLevel level) {
        return level.dimension() == Level.NETHER || (level.dimensionType().hasCeiling() && level.dimensionType().ultraWarm());
    }

    public static boolean isEnd(final ServerLevel level) {
        return level.dimension() == Level.END;
    }

    /** Bukkit-style environment name used by messages and signs. */
    public static String environment(final ServerLevel level) {
        if (isNether(level)) {
            return "NETHER";
        }
        if (isEnd(level)) {
            return "THE_END";
        }
        return "NORMAL";
    }
}
