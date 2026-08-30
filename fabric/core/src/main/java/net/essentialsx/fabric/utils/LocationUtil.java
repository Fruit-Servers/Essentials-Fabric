package net.essentialsx.fabric.utils;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.LazyLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Teleport safety resolver (Section 8.2), ported from upstream LocationUtil and adapted
 * to 1.21.1 block states.
 */
public final class LocationUtil {
    public static final int RADIUS = 3;
    public static final Vector3D[] VOLUME;
    private static final Set<Block> DAMAGING_TYPES = new HashSet<>(List.of(
        Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.FIRE, Blocks.MAGMA_BLOCK, Blocks.SOUL_CAMPFIRE, Blocks.SOUL_FIRE,
        Blocks.SWEET_BERRY_BUSH, Blocks.WITHER_ROSE, Blocks.POWDER_SNOW, Blocks.LAVA, Blocks.POINTED_DRIPSTONE));
    private static volatile boolean waterSafe = false;

    static {
        final List<Vector3D> pos = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    pos.add(new Vector3D(x, y, z));
                }
            }
        }
        pos.sort(Comparator.comparingInt(a -> a.x * a.x + a.y * a.y + a.z * a.z));
        VOLUME = pos.toArray(new Vector3D[0]);
    }

    private LocationUtil() {
    }

    public static void setIsWaterSafe(final boolean isWaterSafe) {
        waterSafe = isWaterSafe;
    }

    /**
     * Whether a block can be occupied by a player body part (hollow / passable).
     */
    public static boolean isHollow(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return isHollow(level, pos, state);
    }

    public static boolean isHollow(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (state.isAir()) {
            return true;
        }
        final Block block = state.getBlock();
        if (block == Blocks.BARRIER || block == Blocks.DIRT_PATH || block == Blocks.FARMLAND) {
            return false;
        }
        if (block == Blocks.LIGHT) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) {
                return waterSafe;
            }
            return false;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Whether the block is "transparent" for the purposes of finding the targeted block.
     */
    public static boolean isTransparent(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static LazyLocation getTarget(final LivingEntity entity) throws Exception {
        return getTarget(entity, 300);
    }

    public static LazyLocation getTarget(final LivingEntity entity, final int maxDistance) throws Exception {
        final BlockPos pos = getTargetBlock(entity, maxDistance);
        if (pos == null) {
            throw new Exception("Not targeting a block");
        }
        return LazyLocation.of((ServerLevel) entity.level(), pos);
    }

    public static BlockPos getTargetBlock(final LivingEntity entity, final int maxDistance) {
        final Vec3 eye = entity.getEyePosition();
        final Vec3 look = entity.getViewVector(1.0f);
        final Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
        final BlockHitResult hit = entity.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }

    public static BlockHitResult getTargetBlockHit(final LivingEntity entity, final int maxDistance) {
        final Vec3 eye = entity.getEyePosition();
        final Vec3 look = entity.getViewVector(1.0f);
        final Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
        return entity.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
    }

    public static boolean isBlockAboveAir(final ServerLevel world, final int x, final int y, final int z) {
        return y > world.getMaxBuildHeight() || isHollow(world, new BlockPos(x, y - 1, z));
    }

    public static boolean isBlockOutsideWorldBorder(final ServerLevel world, final int x, final int z) {
        final WorldBorder border = world.getWorldBorder();
        final int radius = (int) border.getSize() / 2;
        final int cx = (int) Math.floor(border.getCenterX());
        final int cz = (int) Math.floor(border.getCenterZ());
        final int x1 = cx - radius, x2 = cx + radius;
        final int z1 = cz - radius, z2 = cz + radius;
        return x < x1 || x > x2 || z < z1 || z > z2;
    }

    public static int getXInsideWorldBorder(final ServerLevel world, final int x) {
        final WorldBorder border = world.getWorldBorder();
        final int radius = (int) border.getSize() / 2;
        final int cx = (int) Math.floor(border.getCenterX());
        final int x1 = cx - radius, x2 = cx + radius;
        if (x < x1) {
            return x1;
        } else if (x > x2) {
            return x2;
        }
        return x;
    }

    public static int getZInsideWorldBorder(final ServerLevel world, final int z) {
        final WorldBorder border = world.getWorldBorder();
        final int radius = (int) border.getSize() / 2;
        final int cz = (int) Math.floor(border.getCenterZ());
        final int z1 = cz - radius, z2 = cz + radius;
        if (z < z1) {
            return z1;
        } else if (z > z2) {
            return z2;
        }
        return z;
    }

    public static boolean isBlockUnsafeForUser(final Essentials ess, final IUser user, final ServerLevel world, final int x, final int y, final int z) {
        final ServerPlayer base = user.getBase();
        if (base != null && world == base.level() && (base.gameMode.getGameModeForPlayer() == GameType.CREATIVE || base.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || user.isGodModeEnabled()) && base.getAbilities().mayfly) {
            return false;
        }
        if (isBlockDamaging(world, x, y, z)) {
            return true;
        }
        if (isBlockAboveAir(world, x, y, z)) {
            return true;
        }
        return isBlockOutsideWorldBorder(world, x, z);
    }

    public static boolean isBlockUnsafe(final ServerLevel world, final int x, final int y, final int z) {
        return isBlockDamaging(world, x, y, z) || isBlockAboveAir(world, x, y, z);
    }

    private static boolean isBlockUnsafe(final ServerLevel world, final int x, final int y, final int z, final int maxY) {
        return y >= maxY || isBlockUnsafe(world, x, y, z);
    }

    public static boolean isBlockDamaging(final ServerLevel world, final int x, final int y, final int z) {
        final BlockPos pos = new BlockPos(x, y, z);
        final BlockState block = world.getBlockState(pos);
        final BlockState below = world.getBlockState(pos.below());
        final BlockState above = world.getBlockState(pos.above());
        if (DAMAGING_TYPES.contains(below.getBlock()) || below.getFluidState().is(Fluids.LAVA) || below.getFluidState().is(Fluids.FLOWING_LAVA) || below.is(BlockTags.BEDS)) {
            return true;
        }
        if (block.is(Blocks.NETHER_PORTAL) || block.is(Blocks.END_PORTAL)) {
            return true;
        }
        if (!block.getFluidState().isEmpty() && (block.getFluidState().is(Fluids.LAVA) || block.getFluidState().is(Fluids.FLOWING_LAVA))) {
            return true;
        }
        return !isHollow(world, pos, block) || !isHollow(world, pos.above(), above);
    }

    public static LazyLocation getRoundedDestination(final LazyLocation loc) {
        final int x = loc.blockX();
        final int y = (int) Math.round(loc.y());
        final int z = loc.blockZ();
        return loc.withPosition(x + 0.5, y, z + 0.5);
    }

    public static LazyLocation getSafeDestination(final Essentials ess, final IUser user, final LazyLocation loc) throws Exception {
        final ServerPlayer base = user.getBase();
        if (base != null && (ess == null || !ess.getSettings().isAlwaysTeleportSafety()) && (base.gameMode.getGameModeForPlayer() == GameType.CREATIVE || base.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || user.isGodModeEnabled())) {
            final ServerLevel level = loc.level(ess.getServer());
            if (level != null && shouldFly(level, loc) && base.getAbilities().mayfly) {
                base.getAbilities().flying = true;
                base.onUpdateAbilities();
            }
            if (ess == null || ess.getSettings().isTeleportToCenterLocation()) {
                return getRoundedDestination(loc);
            } else {
                return loc;
            }
        }
        return getSafeDestination(ess, loc);
    }

    public static LazyLocation getSafeDestination(final Essentials ess, final LazyLocation loc) throws Exception {
        if (loc == null) {
            throw new TranslatableException("destinationNotSet");
        }
        final ServerLevel world = loc.level(ess.getServer());
        if (world == null) {
            throw new TranslatableException("destinationNotSet");
        }
        final int worldMinY = world.getMinBuildHeight();
        final int worldLogicalY = world.getLogicalHeight() + worldMinY;
        final int worldMaxY = ess.getSettings().isConsiderWorldHeightForTeleportSafety() && loc.blockY() < worldLogicalY
            ? worldLogicalY
            : world.getMaxBuildHeight();
        int x = loc.blockX();
        int y = (int) Math.round(loc.y());
        int z = loc.blockZ();
        if (isBlockOutsideWorldBorder(world, x, z)) {
            x = getXInsideWorldBorder(world, x);
            z = getZInsideWorldBorder(world, z);
        }
        final int origX = x;
        final int origY = y;
        final int origZ = z;
        while (isBlockAboveAir(world, x, y, z)) {
            y -= 1;
            if (y < worldMinY) {
                y = origY;
                break;
            }
        }
        if (isBlockUnsafe(world, x, y, z, worldMaxY)) {
            x = Math.round(loc.x()) == origX ? x - 1 : x + 1;
            z = Math.round(loc.z()) == origZ ? z - 1 : z + 1;
        }
        int i = 0;
        while (isBlockUnsafe(world, x, y, z, worldMaxY)) {
            i++;
            if (i >= VOLUME.length) {
                x = origX;
                y = NumberUtil.constrainToRange(origY + RADIUS, worldMinY, worldMaxY);
                z = origZ;
                break;
            }
            x = origX + VOLUME[i].x;
            y = NumberUtil.constrainToRange(origY + VOLUME[i].y, worldMinY, worldMaxY);
            z = origZ + VOLUME[i].z;
        }
        while (isBlockUnsafe(world, x, y, z, worldMaxY)) {
            y += 1;
            if (y >= worldMaxY) {
                x += 1;
                break;
            }
        }
        while (isBlockUnsafe(world, x, y, z, worldMaxY)) {
            y -= 1;
            if (y <= worldMinY + 1) {
                x += 1;
                y = Math.min(world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1, worldMaxY);
                if (x - 48 > loc.blockX()) {
                    throw new TranslatableException("holeInFloor");
                }
            }
        }
        return new LazyLocation(loc.world(), loc.worldName(), x + 0.5, y, z + 0.5, loc.yaw(), loc.pitch());
    }

    public static boolean shouldFly(final ServerLevel world, final LazyLocation loc) {
        final int x = loc.blockX();
        int y = (int) Math.round(loc.y());
        final int z = loc.blockZ();
        int count = 0;
        while (LocationUtil.isBlockUnsafe(world, x, y, z) && y >= world.getMinBuildHeight()) {
            y--;
            count++;
            if (count > 2) {
                return true;
            }
        }
        return y < world.getMinBuildHeight();
    }

    public static int getHighestBlockY(final ServerLevel world, final int x, final int z) {
        return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    public static class Vector3D {
        public final int x;
        public final int y;
        public final int z;

        Vector3D(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
