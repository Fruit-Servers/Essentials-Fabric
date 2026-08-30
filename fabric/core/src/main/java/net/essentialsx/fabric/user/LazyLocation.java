package net.essentialsx.fabric.user;

import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Persisted location reference (Section 6.2): dimension key + coordinates + orientation.
 * No live world object is retained. {@code world} stores the dimension key
 * (e.g. {@code minecraft:overworld}); {@code worldName} stores a legacy/display name
 * for migration from Bukkit data.
 */
public final class LazyLocation {
    private String world;
    private String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public LazyLocation(final String worldId, final String worldName, final double x, final double y, final double z, final float yaw, final float pitch) {
        this.world = worldId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static LazyLocation of(final ServerLevel level, final double x, final double y, final double z, final float yaw, final float pitch) {
        return new LazyLocation(Worlds.key(level), Worlds.name(level), x, y, z, yaw, pitch);
    }

    public static LazyLocation of(final ServerLevel level, final Vec3 pos) {
        return of(level, pos.x, pos.y, pos.z, 0f, 0f);
    }

    public static LazyLocation of(final ServerLevel level, final BlockPos pos) {
        return of(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
    }

    public static LazyLocation of(final Entity entity) {
        return of((ServerLevel) entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    public static LazyLocation of(final ServerPlayer player) {
        return of((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public static LazyLocation fromMap(final Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        final Object world = map.get("world");
        if (world == null) {
            return null;
        }
        final Object worldName = map.get("world-name");
        return new LazyLocation(world.toString(), worldName == null ? null : worldName.toString(),
            num(map.get("x")), num(map.get("y")), num(map.get("z")), (float) num(map.get("yaw")), (float) num(map.get("pitch")));
    }

    private static double num(final Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (final NumberFormatException ignored) {
            }
        }
        return 0d;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", world);
        if (worldName != null) {
            map.put("world-name", worldName);
        }
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }

    public String world() {
        return world;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    public BlockPos blockPos() {
        return new BlockPos(blockX(), blockY(), blockZ());
    }

    public Vec3 vec() {
        return new Vec3(x, y, z);
    }

    /**
     * Resolve the dimension on the given server, or null when unavailable.
     */
    public ServerLevel level(final MinecraftServer server) {
        if (server == null || world == null || world.isEmpty()) {
            return null;
        }
        ServerLevel level = Worlds.get(server, world);
        if (level == null && worldName != null && !worldName.isEmpty()) {
            level = Worlds.get(server, worldName);
        }
        if (level != null) {
            this.world = Worlds.key(level);
            this.worldName = Worlds.name(level);
        }
        return level;
    }

    public boolean isAvailable(final MinecraftServer server) {
        return level(server) != null;
    }

    /**
     * Display name of the world (dimension) for messages.
     */
    public String worldDisplayName(final MinecraftServer server) {
        final ServerLevel level = level(server);
        if (level != null) {
            return Worlds.name(level);
        }
        return worldName != null && !worldName.isEmpty() ? worldName : world;
    }

    public LazyLocation withPosition(final double x, final double y, final double z) {
        return new LazyLocation(world, worldName, x, y, z, yaw, pitch);
    }

    public LazyLocation withRotation(final float yaw, final float pitch) {
        return new LazyLocation(world, worldName, x, y, z, yaw, pitch);
    }

    public LazyLocation withLevel(final ServerLevel level) {
        return new LazyLocation(Worlds.key(level), Worlds.name(level), x, y, z, yaw, pitch);
    }

    public LazyLocation add(final double dx, final double dy, final double dz) {
        return withPosition(x + dx, y + dy, z + dz);
    }

    public double distanceSquared(final LazyLocation other) {
        final double dx = x - other.x;
        final double dy = y - other.y;
        final double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(final LazyLocation other) {
        return Math.sqrt(distanceSquared(other));
    }

    public boolean sameWorld(final LazyLocation other) {
        return other != null && Objects.equals(world, other.world);
    }

    public boolean sameBlock(final LazyLocation other) {
        return sameWorld(other) && blockX() == other.blockX() && blockY() == other.blockY() && blockZ() == other.blockZ();
    }

    @Override
    public String toString() {
        return "LazyLocation{" + world + " " + x + "," + y + "," + z + " yaw=" + yaw + " pitch=" + pitch + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof LazyLocation)) return false;
        final LazyLocation that = (LazyLocation) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Double.compare(that.z, z) == 0 && Float.compare(that.yaw, yaw) == 0 && Float.compare(that.pitch, pitch) == 0 && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z, yaw, pitch);
    }
}
