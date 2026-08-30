package net.essentialsx.fabric.rtp;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Random teleport definitions from {@code tpr.yml} (Section 8.6). Candidate positions load
 * chunks asynchronously and are validated on the server thread.
 */
public class RandomTeleport {
    private static final Random RANDOM = new Random();
    private static final int HIGHEST_BLOCK_Y_OFFSET = 1;
    private final Essentials ess;
    private final YamlFile config;
    private final Map<String, ConcurrentLinkedQueue<LazyLocation>> cachedLocations = new HashMap<>();

    public RandomTeleport(final Essentials essentials) {
        this.ess = essentials;
        config = new YamlFile(essentials.getDataFolder().resolve("tpr.yml"), "/tpr.yml",
            "Configuration for the random teleport command.\nUse the /settpr command in-game to set random teleport locations.");
        reloadConfig();
    }

    public YamlFile getConfig() {
        return config;
    }

    public void reloadConfig() {
        config.load();
        cachedLocations.clear();
    }

    public boolean hasLocation(final String name) {
        return config.hasProperty("locations." + name);
    }

    public LazyLocation getCenter(final String name) {
        final LazyLocation center = config.getLocation(locationKey(name, "center"));
        if (center != null && center.isAvailable(ess.getServer())) {
            return center;
        }
        final ServerLevel level = ess.getOverworld();
        final int x = (int) Math.floor(level.getWorldBorder().getCenterX());
        final int z = (int) Math.floor(level.getWorldBorder().getCenterZ());
        final LazyLocation worldCenter = LazyLocation.of(level, x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + HIGHEST_BLOCK_Y_OFFSET, z + 0.5, 0f, 0f);
        setCenter(name, worldCenter);
        return worldCenter;
    }

    public void setCenter(final String name, final LazyLocation center) {
        config.setProperty(locationKey(name, "center"), center.toMap());
        config.save();
        this.getCachedLocations(name).clear();
    }

    public double getMinRange(final String name) {
        return config.getDouble(locationKey(name, "min-range"), 0d);
    }

    public void setMinRange(final String name, final double minRange) {
        config.setProperty(locationKey(name, "min-range"), minRange);
        config.save();
        this.getCachedLocations(name).clear();
    }

    public double getMaxRange(final String name) {
        final ServerLevel level = getCenter(name).level(ess.getServer());
        return config.getDouble(locationKey(name, "max-range"), (level == null ? ess.getOverworld() : level).getWorldBorder().getSize() / 2);
    }

    public void setMaxRange(final String name, final double maxRange) {
        config.setProperty(locationKey(name, "max-range"), maxRange);
        config.save();
        this.getCachedLocations(name).clear();
    }

    public String getDefaultLocation() {
        return config.getString("default-location", "{world}");
    }

    public boolean isPerLocationPermission() {
        return config.getBoolean("per-location-permission", false);
    }

    public Set<String> getExcludedBiomes() {
        final Set<String> excludedBiomes = new HashSet<>();
        for (final String key : config.getStringList("excluded-biomes")) {
            excludedBiomes.add(key.toLowerCase(Locale.ENGLISH));
        }
        return excludedBiomes;
    }

    public int getFindAttempts() {
        return config.getInt("find-attempts", 10);
    }

    public int getCacheThreshold() {
        return config.getInt("cache-threshold", 10);
    }

    public boolean isLegacyCenterFallback() {
        return config.getBoolean("legacy-center-fallback", false);
    }

    public List<String> listLocations() {
        return new ArrayList<>(config.getKeys("locations"));
    }

    public Queue<LazyLocation> getCachedLocations(final String name) {
        return cachedLocations.computeIfAbsent(name, x -> new ConcurrentLinkedQueue<>());
    }

    public CompletableFuture<LazyLocation> getRandomLocation(final String name) {
        final Queue<LazyLocation> cached = this.getCachedLocations(name);
        if (cached.size() < this.getCacheThreshold()) {
            cacheRandomLocations(name);
        }
        final CompletableFuture<LazyLocation> future = new CompletableFuture<>();
        if (cached.isEmpty()) {
            final int findAttempts = this.getFindAttempts();
            final LazyLocation center = this.getCenter(name);
            final double minRange = this.getMinRange(name);
            final double maxRange = this.getMaxRange(name);
            attemptRandomLocation(findAttempts, center, minRange, maxRange).whenComplete((loc, t) -> {
                if (t != null) {
                    future.completeExceptionally(t);
                } else {
                    future.complete(loc);
                }
            });
        } else {
            future.complete(cached.poll());
        }
        return future;
    }

    public CompletableFuture<LazyLocation> getRandomLocation(final LazyLocation center, final double minRange, final double maxRange) {
        return attemptRandomLocation(this.getFindAttempts(), center, minRange, maxRange);
    }

    public void cacheRandomLocations(final String name) {
        ess.scheduleSyncDelayedTask(() -> {
            for (int i = 0; i < this.getFindAttempts(); ++i) {
                calculateRandomLocation(getCenter(name), getMinRange(name), getMaxRange(name)).thenAccept(location -> {
                    if (location != null && isValidRandomLocation(location)) {
                        this.getCachedLocations(name).add(location);
                    }
                });
            }
        });
    }

    private CompletableFuture<LazyLocation> attemptRandomLocation(final int attempts, final LazyLocation center, final double minRange, final double maxRange) {
        final CompletableFuture<LazyLocation> future = new CompletableFuture<>();
        if (attempts > 0) {
            calculateRandomLocation(center, minRange, maxRange).thenAccept(location -> {
                if (location != null && isValidRandomLocation(location)) {
                    future.complete(location);
                } else {
                    attemptRandomLocation(attempts - 1, center, minRange, maxRange).whenComplete((loc, t) -> {
                        if (t != null) {
                            future.completeExceptionally(t);
                        } else {
                            future.complete(loc);
                        }
                    });
                }
            });
        } else if (isLegacyCenterFallback()) {
            future.complete(center);
        } else {
            future.completeExceptionally(new net.essentialsx.fabric.text.TranslatableException("noSafeLocationsFound"));
        }
        return future;
    }

    private CompletableFuture<LazyLocation> calculateRandomLocation(final LazyLocation center, final double minRange, final double maxRange) {
        final CompletableFuture<LazyLocation> future = new CompletableFuture<>();
        final ServerLevel level = center.level(ess.getServer());
        if (level == null) {
            future.complete(null);
            return future;
        }
        final double rectX = RANDOM.nextDouble() * (maxRange - minRange) + minRange;
        final double rectZ = RANDOM.nextDouble() * (maxRange + minRange) - minRange;
        final double offsetX;
        final double offsetZ;
        final int transform = RANDOM.nextInt(4);
        if (transform == 0) {
            offsetX = rectX;
            offsetZ = rectZ;
        } else if (transform == 1) {
            offsetX = -rectZ;
            offsetZ = rectX;
        } else if (transform == 2) {
            offsetX = -rectX;
            offsetZ = -rectZ;
        } else {
            offsetX = rectZ;
            offsetZ = -rectX;
        }
        final double x = center.x() + offsetX;
        final double z = center.z() + offsetZ;
        final float yaw = 360 * RANDOM.nextFloat() - 180;
        final ChunkPos chunkPos = new ChunkPos(new BlockPos((int) Math.floor(x), 0, (int) Math.floor(z)));
        level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, 0);
        level.getChunkSource().getChunkFuture(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true).whenCompleteAsync((chunk, throwable) -> {
            if (throwable != null) {
                future.complete(null);
                return;
            }
            final double y;
            if (Worlds.isNether(level)) {
                y = getNetherYAt(level, (int) Math.floor(x), (int) Math.floor(z));
            } else {
                y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z)) + HIGHEST_BLOCK_Y_OFFSET;
            }
            future.complete(LazyLocation.of(level, x, y, z, yaw, 0f));
        }, ess.getServer());
        return future;
    }

    private double getNetherYAt(final ServerLevel world, final int x, final int z) {
        for (int y = 32; y < world.getMaxBuildHeight(); ++y) {
            if (world.getBlockState(new BlockPos(x, y, z)).is(Blocks.BEDROCK)) {
                break;
            }
            if (!LocationUtil.isBlockUnsafe(world, x, y, z)) {
                return y;
            }
        }
        return Double.MIN_VALUE;
    }

    private boolean isValidRandomLocation(final LazyLocation location) {
        final ServerLevel level = location.level(ess.getServer());
        if (level == null) {
            return false;
        }
        if (LocationUtil.isBlockOutsideWorldBorder(level, location.blockX(), location.blockZ())) {
            return false;
        }
        return location.blockY() > level.getMinBuildHeight() && !isExcludedBiome(level, location);
    }

    private boolean isExcludedBiome(final ServerLevel level, final LazyLocation location) {
        final Set<String> excluded = getExcludedBiomes();
        if (excluded.isEmpty()) {
            return false;
        }
        final Holder<Biome> biome = level.getBiome(location.blockPos());
        final String key = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
        final String path = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        return excluded.contains(key) || excluded.contains(path) || excluded.contains(path.toUpperCase(Locale.ENGLISH).toLowerCase(Locale.ENGLISH));
    }

    private String locationKey(final String name, final String key) {
        return "locations." + name + "." + key;
    }

    public Path getFile() {
        return config.getFile();
    }
}
