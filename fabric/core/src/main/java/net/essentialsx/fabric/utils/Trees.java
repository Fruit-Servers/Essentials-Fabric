package net.essentialsx.fabric.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tree generation for {@code /tree} and {@code /bigtree} using 1.21.1 configured features.
 */
public final class Trees {
    private static final Map<String, ResourceKey<ConfiguredFeature<?, ?>>> TYPES = Map.ofEntries(
        Map.entry("tree", TreeFeatures.OAK),
        Map.entry("oak", TreeFeatures.OAK),
        Map.entry("birch", TreeFeatures.BIRCH),
        Map.entry("redwood", TreeFeatures.SPRUCE),
        Map.entry("spruce", TreeFeatures.SPRUCE),
        Map.entry("redmushroom", TreeFeatures.HUGE_RED_MUSHROOM),
        Map.entry("brownmushroom", TreeFeatures.HUGE_BROWN_MUSHROOM),
        Map.entry("jungle", TreeFeatures.JUNGLE_TREE),
        Map.entry("junglebush", TreeFeatures.JUNGLE_BUSH),
        Map.entry("swamp", TreeFeatures.SWAMP_OAK),
        Map.entry("acacia", TreeFeatures.ACACIA),
        Map.entry("darkoak", TreeFeatures.DARK_OAK),
        Map.entry("cherry", TreeFeatures.CHERRY),
        Map.entry("mangrove", TreeFeatures.MANGROVE),
        Map.entry("azalea", TreeFeatures.AZALEA_TREE)
    );
    private static final Map<String, ResourceKey<ConfiguredFeature<?, ?>>> BIG_TYPES = Map.of(
        "tree", TreeFeatures.FANCY_OAK,
        "redwood", TreeFeatures.MEGA_SPRUCE,
        "jungle", TreeFeatures.MEGA_JUNGLE_TREE,
        "darkoak", TreeFeatures.DARK_OAK,
        "mangrove", TreeFeatures.TALL_MANGROVE
    );

    private Trees() {
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> small(final String name) {
        return TYPES.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> big(final String name) {
        return BIG_TYPES.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static boolean generate(final ServerLevel level, final BlockPos pos, final ResourceKey<ConfiguredFeature<?, ?>> key) {
        final Optional<Holder.Reference<ConfiguredFeature<?, ?>>> holder = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key);
        if (holder.isEmpty()) {
            return false;
        }
        return holder.get().value().place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos);
    }
}
