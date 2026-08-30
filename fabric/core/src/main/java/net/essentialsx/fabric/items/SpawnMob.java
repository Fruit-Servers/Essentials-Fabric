package net.essentialsx.fabric.items;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Mob spawning with Essentials data syntax and permission/limit checks.
 */
public final class SpawnMob {
    private SpawnMob() {
    }

    public static String mobList(final User user) {
        final Set<String> mobList = Mob.getMobList();
        final Set<String> availableList = new HashSet<>();
        for (final String mob : mobList) {
            if (user.isAuthorized("essentials.spawnmob." + mob.toLowerCase(Locale.ENGLISH))) {
                availableList.add(mob);
            }
        }
        if (availableList.isEmpty()) {
            availableList.add(user.playerTl("none"));
        }
        return StringUtil.joinList(availableList);
    }

    public static List<String> mobParts(final String mobString) {
        final String[] mobParts = mobString.split(",");
        final List<String> mobs = new ArrayList<>();
        for (final String mobPart : mobParts) {
            final String[] mobDatas = mobPart.split(":");
            mobs.add(mobDatas[0]);
        }
        return mobs;
    }

    public static List<String> mobData(final String mobString) {
        final String[] mobParts = mobString.split(",");
        final List<String> mobData = new ArrayList<>();
        for (final String mobPart : mobParts) {
            final String[] mobDatas = mobPart.split(":");
            if (mobDatas.length == 1) {
                if (mobPart.contains(":")) {
                    mobData.add("");
                } else {
                    mobData.add(null);
                }
            } else {
                mobData.add(mobDatas[1]);
            }
        }
        return mobData;
    }

    public static void spawnmob(final Essentials ess, final User user, final List<String> parts, final List<String> data, final int mobCount) throws Exception {
        final BlockPos block = LocationUtil.getTargetBlock(user.getBase(), 300);
        if (block == null) {
            throw new TranslatableException("unableToSpawnMob");
        }
        spawnmob(ess, user.getSource(), user, LazyLocation.of(user.getWorld(), block.above()), parts, data, mobCount);
    }

    public static void spawnmob(final Essentials ess, final CommandSource sender, final User target, final List<String> parts, final List<String> data, final int mobCount) throws Exception {
        spawnmob(ess, sender, target, target.getLocation(), parts, data, mobCount);
    }

    public static void spawnmob(final Essentials ess, final CommandSource sender, final User target, final LazyLocation loc, final List<String> parts, final List<String> data, int mobCount) throws Exception {
        final LazyLocation sloc = LocationUtil.getSafeDestination(ess, loc);
        for (final String part : parts) {
            final Mob mob = Mob.fromName(part);
            checkSpawnable(ess, sender, mob);
        }
        final int serverLimit = ess.getSettings().getSpawnMobLimit();
        int effectiveLimit = serverLimit / parts.size();
        if (effectiveLimit < 1) {
            effectiveLimit = 1;
            while (parts.size() > serverLimit) {
                parts.remove(serverLimit);
            }
        }
        if (mobCount > effectiveLimit) {
            mobCount = effectiveLimit;
            sender.sendTl("mobSpawnLimit");
        }
        final Mob mob = Mob.fromName(parts.get(0));
        try {
            for (int i = 0; i < mobCount; i++) {
                spawnMob(ess, sender, target, sloc, parts, data);
            }
            sender.sendMessage(mobCount * parts.size() + " " + mob.name.toLowerCase(Locale.ENGLISH) + mob.suffix + " " + sender.tl("spawned"));
        } catch (final Mob.MobException e1) {
            throw new TranslatableException(e1, "unableToSpawnMob");
        } catch (final NumberFormatException e2) {
            throw new TranslatableException(e2, "numberRequired");
        } catch (final NullPointerException np) {
            throw new TranslatableException(np, "soloMob");
        }
    }

    private static void spawnMob(final Essentials ess, final CommandSource sender, final User target, final LazyLocation sloc, final List<String> parts, final List<String> data) throws Exception {
        final ServerLevel level = sloc.level(ess.getServer());
        if (level == null) {
            throw new TranslatableException("invalidWorld");
        }
        Mob mob;
        Entity spawnedMob = null;
        Entity spawnedMount;
        for (int i = 0; i < parts.size(); i++) {
            if (i == 0) {
                mob = Mob.fromName(parts.get(i));
                spawnedMob = mob.spawn(level, sloc.x(), sloc.y(), sloc.z());
                defaultMobData(mob.getType(), spawnedMob);
                if (data.get(i) != null) {
                    changeMobData(sender, mob.getType(), spawnedMob, data.get(i).toLowerCase(Locale.ENGLISH), target);
                }
            }
            final int next = i + 1;
            if (next < parts.size()) {
                final Mob mMob = Mob.fromName(parts.get(next));
                spawnedMount = mMob.spawn(level, sloc.x(), sloc.y(), sloc.z());
                defaultMobData(mMob.getType(), spawnedMount);
                if (data.get(next) != null) {
                    changeMobData(sender, mMob.getType(), spawnedMount, data.get(next).toLowerCase(Locale.ENGLISH), target);
                }
                spawnedMount.startRiding(spawnedMob, true);
                spawnedMob = spawnedMount;
            }
        }
    }

    private static void checkSpawnable(final Essentials ess, final CommandSource sender, final Mob mob) throws Exception {
        if (mob == null || mob.getType() == null) {
            throw new TranslatableException("invalidMob");
        }
        if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.spawnmob." + mob.name.toLowerCase(Locale.ENGLISH))) {
            throw new TranslatableException("noPermToSpawnMob");
        }
    }

    private static void changeMobData(final CommandSource sender, final EntityType<?> type, final Entity spawned, final String inputData, final User target) throws Exception {
        String data = inputData;
        if (data.isEmpty()) {
            sender.sendTl("mobDataList", StringUtil.joinList(MobData.getValidHelp(spawned)));
        }
        if (spawned instanceof Zombie zombie) {
            zombie.setBaby(false);
        } else if (spawned instanceof AgeableMob ageable) {
            ageable.setBaby(false);
        }
        if (spawned instanceof Zombie || type == EntityType.SKELETON) {
            if (inputData.contains("armor") || inputData.contains("armour")) {
                final LivingEntity living = (LivingEntity) spawned;
                if (inputData.contains("noarmor") || inputData.contains("noarmour")) {
                    for (final EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.isArmor()) {
                            living.setItemSlot(slot, ItemStack.EMPTY);
                        }
                    }
                } else {
                    final String material = inputData.contains("netherite") ? "netherite" : inputData.contains("diamond") ? "diamond" : inputData.contains("gold") ? "golden" : inputData.contains("leather") ? "leather" : inputData.contains("iron") ? "iron" : null;
                    if (material != null) {
                        living.setItemSlot(EquipmentSlot.FEET, new ItemStack(item(material + "_boots")));
                        living.setItemSlot(EquipmentSlot.LEGS, new ItemStack(item(material + "_leggings")));
                        living.setItemSlot(EquipmentSlot.CHEST, new ItemStack(item(material + "_chestplate")));
                        living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item(material + "_helmet")));
                    }
                }
                if (living instanceof net.minecraft.world.entity.Mob mob) {
                    for (final EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.isArmor()) {
                            mob.setDropChance(slot, 0f);
                        }
                    }
                }
            }
        }
        MobData newData = MobData.fromData(spawned, data);
        int guard = 0;
        while (newData != null && guard++ < 20) {
            newData.setData(spawned, target.getBase(), data);
            data = data.replace(newData.getMatched(), "");
            newData = MobData.fromData(spawned, data);
        }
    }

    private static net.minecraft.world.item.Item item(final String key) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(key));
    }

    private static void defaultMobData(final EntityType<?> type, final Entity spawned) {
        if (type == EntityType.SKELETON && spawned instanceof net.minecraft.world.entity.Mob mob) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN && spawned instanceof net.minecraft.world.entity.Mob mob) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        }
        if (type == EntityType.HORSE && spawned instanceof Horse horse) {
            horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH).setBaseValue(1.2);
        }
    }
}
