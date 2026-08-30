package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Mob;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.Locale;

public class Commandspawner extends EssentialsCommand {
    public Commandspawner() {
        super("spawner");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1 || args[0].length() < 2) {
            throw new NotEnoughArgumentsException(user.playerTl("mobsAvailable", StringUtil.joinList(Mob.getMobList().toArray())));
        }
        final BlockPos target = LocationUtil.getTargetBlock(user.getBase(), 300);
        final ServerLevel level = user.getWorld();
        final BlockEntity blockEntity = target == null ? null : level.getBlockEntity(target);
        if (!(blockEntity instanceof SpawnerBlockEntity spawnerEntity)) {
            throw new TranslatableException("mobSpawnTarget");
        }
        final String name = args[0];
        int delay = 0;
        final Mob mob = Mob.fromName(name);
        if (mob == null) {
            throw new TranslatableException("invalidMob");
        }
        if (!user.isAuthorized("essentials.spawner." + mob.name.toLowerCase(Locale.ENGLISH))) {
            throw new TranslatableException("noPermToSpawnMob");
        }
        if (args.length > 1 && NumberUtil.isInt(args[1]) && user.isAuthorized("essentials.spawner.delay")) {
            delay = Integer.parseInt(args[1]);
        }
        final Trade charge = new Trade("spawner-" + mob.name.toLowerCase(Locale.ENGLISH), ess);
        charge.isAffordableFor(user);
        try {
            spawnerEntity.setEntityId(mob.getType(), level.getRandom());
            if (delay > 0) {
                final CompoundTag tag = spawnerEntity.getSpawner().save(new CompoundTag());
                tag.putShort("MinSpawnDelay", (short) Math.min(delay, Short.MAX_VALUE));
                tag.putShort("MaxSpawnDelay", (short) Math.min(delay, Short.MAX_VALUE));
                tag.putShort("Delay", (short) Math.min(delay, Short.MAX_VALUE));
                spawnerEntity.getSpawner().load(level, target, tag);
            }
            spawnerEntity.setChanged();
            level.sendBlockUpdated(target, level.getBlockState(target), level.getBlockState(target), 3);
        } catch (final Throwable ex) {
            throw new TranslatableException(ex, "mobSpawnError");
        }
        charge.charge(user);
        user.sendTl("setSpawner", mob.name);
    }
}
