package net.essentialsx.fabric.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Essentials mob name → entity type table (registry-backed; missing types are skipped).
 */
public final class Mob {
    private static final Map<String, Mob> BY_NAME = new LinkedHashMap<>();
    private static final Map<EntityType<?>, Mob> BY_TYPE = new HashMap<>();

    static {
        final String[][] table = {
            {"Chicken", "friendly", "s", "chicken"}, {"Cow", "friendly", "s", "cow"}, {"Creeper", "enemy", "s", "creeper"},
            {"Ghast", "enemy", "s", "ghast"}, {"Giant", "enemy", "s", "giant"}, {"Horse", "friendly", "s", "horse"},
            {"Pig", "friendly", "s", "pig"}, {"PigZombie", "neutral", "s", "zombified_piglin"}, {"ZombifiedPiglin", "neutral", "s", "zombified_piglin"},
            {"Sheep", "friendly", "", "sheep"}, {"Skeleton", "enemy", "s", "skeleton"}, {"Slime", "enemy", "s", "slime"},
            {"Spider", "enemy", "s", "spider"}, {"Squid", "friendly", "s", "squid"}, {"Zombie", "enemy", "s", "zombie"},
            {"Wolf", "neutral", "", "wolf"}, {"CaveSpider", "enemy", "s", "cave_spider"}, {"Enderman", "enemy", "", "enderman"},
            {"Silverfish", "enemy", "", "silverfish"}, {"EnderDragon", "enemy", "s", "ender_dragon"}, {"Villager", "friendly", "s", "villager"},
            {"Blaze", "enemy", "s", "blaze"}, {"MushroomCow", "friendly", "s", "mooshroom"}, {"MagmaCube", "enemy", "s", "magma_cube"},
            {"Snowman", "friendly", "", "snow_golem"}, {"Ocelot", "neutral", "s", "ocelot"}, {"IronGolem", "neutral", "s", "iron_golem"},
            {"Wither", "enemy", "s", "wither"}, {"Bat", "friendly", "s", "bat"}, {"Witch", "enemy", "s", "witch"},
            {"Boat", "neutral", "s", "boat"}, {"ChestBoat", "neutral", "s", "chest_boat"},
            {"Minecart", "neutral", "s", "minecart"}, {"ChestMinecart", "neutral", "s", "chest_minecart"}, {"FurnaceMinecart", "neutral", "s", "furnace_minecart"},
            {"TNTMinecart", "neutral", "s", "tnt_minecart"}, {"HopperMinecart", "neutral", "s", "hopper_minecart"}, {"SpawnerMinecart", "neutral", "s", "spawner_minecart"},
            {"EnderCrystal", "neutral", "s", "end_crystal"}, {"ExperienceOrb", "neutral", "s", "experience_orb"}, {"ArmorStand", "neutral", "s", "armor_stand"},
            {"Endermite", "enemy", "s", "endermite"}, {"Guardian", "enemy", "s", "guardian"}, {"ElderGuardian", "enemy", "s", "elder_guardian"},
            {"Rabbit", "friendly", "s", "rabbit"}, {"Shulker", "enemy", "s", "shulker"}, {"PolarBear", "neutral", "s", "polar_bear"},
            {"WitherSkeleton", "enemy", "s", "wither_skeleton"}, {"StraySkeleton", "enemy", "s", "stray"}, {"ZombieVillager", "friendly", "s", "zombie_villager"},
            {"SkeletonHorse", "friendly", "s", "skeleton_horse"}, {"ZombieHorse", "friendly", "s", "zombie_horse"}, {"Donkey", "friendly", "s", "donkey"},
            {"Mule", "friendly", "s", "mule"}, {"Evoker", "enemy", "s", "evoker"}, {"Vex", "enemy", "s", "vex"}, {"Vindicator", "enemy", "s", "vindicator"},
            {"Llama", "neutral", "s", "llama"}, {"Husk", "enemy", "s", "husk"}, {"Illusioner", "enemy", "s", "illusioner"}, {"Parrot", "neutral", "s", "parrot"},
            {"Turtle", "neutral", "s", "turtle"}, {"Phantom", "enemy", "s", "phantom"}, {"Cod", "neutral", "", "cod"}, {"Salmon", "neutral", "", "salmon"},
            {"Pufferfish", "neutral", "", "pufferfish"}, {"TropicalFish", "neutral", "", "tropical_fish"}, {"Drowned", "enemy", "s", "drowned"},
            {"Dolphin", "neutral", "s", "dolphin"}, {"Cat", "friendly", "s", "cat"}, {"Fox", "friendly", "es", "fox"}, {"Panda", "neutral", "s", "panda"},
            {"Pillager", "enemy", "s", "pillager"}, {"Ravager", "enemy", "s", "ravager"}, {"TraderLlama", "friendly", "s", "trader_llama"},
            {"WanderingTrader", "friendly", "s", "wandering_trader"}, {"Bee", "neutral", "s", "bee"}, {"Stray", "enemy", "s", "stray"},
            {"Hoglin", "adult_enemy", "s", "hoglin"}, {"Piglin", "adult_enemy", "s", "piglin"}, {"Strider", "friendly", "s", "strider"},
            {"Zoglin", "enemy", "s", "zoglin"}, {"PiglinBrute", "adult_enemy", "s", "piglin_brute"}, {"Axolotl", "friendly", "s", "axolotl"},
            {"Goat", "neutral", "s", "goat"}, {"GlowSquid", "friendly", "s", "glow_squid"}, {"Allay", "friendly", "s", "allay"},
            {"Frog", "friendly", "s", "frog"}, {"Tadpole", "friendly", "s", "tadpole"}, {"Warden", "enemy", "s", "warden"},
            {"Camel", "friendly", "s", "camel"}, {"Sniffer", "friendly", "s", "sniffer"}, {"Armadillo", "friendly", "s", "armadillo"},
            {"Breeze", "enemy", "s", "breeze"}, {"Bogged", "enemy", "s", "bogged"},
            {"AcaciaBoat", "neutral", "s", "boat"}, {"DarkOakBoat", "neutral", "s", "boat"}, {"BirchBoat", "neutral", "s", "boat"},
            {"JungleBoat", "neutral", "s", "boat"}, {"SpruceBoat", "neutral", "s", "boat"}, {"MangroveBoat", "neutral", "s", "boat"},
            {"CherryBoat", "neutral", "s", "boat"}, {"BambooRaft", "neutral", "s", "boat"},
        };
        for (final String[] row : table) {
            final ResourceLocation loc = ResourceLocation.withDefaultNamespace(row[3]);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
                continue;
            }
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(loc);
            final Mob mob = new Mob(row[0], Enemies.valueOf(row[1].toUpperCase(Locale.ENGLISH)), row[2], type);
            BY_NAME.put(row[0].toLowerCase(Locale.ENGLISH), mob);
            BY_TYPE.putIfAbsent(type, mob);
        }
        // Any registry entity not covered above is reachable by its registry path.
        for (final EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            final String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            final String key = path.replace("_", "").toLowerCase(Locale.ENGLISH);
            if (!BY_NAME.containsKey(key)) {
                final Mob mob = new Mob(path, Enemies.NEUTRAL, "s", type);
                BY_NAME.put(key, mob);
                BY_TYPE.putIfAbsent(type, mob);
            }
        }
    }

    public final String name;
    public final Enemies type;
    public final String suffix;
    private final EntityType<?> entityType;

    private Mob(final String name, final Enemies type, final String suffix, final EntityType<?> entityType) {
        this.name = name;
        this.type = type;
        this.suffix = suffix;
        this.entityType = entityType;
    }

    public static Set<String> getMobList() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }

    public static Mob fromName(final String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ENGLISH).replace("_", ""));
    }

    public static Mob fromType(final EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    public Entity spawn(final ServerLevel level, final double x, final double y, final double z) throws MobException {
        final Entity entity = entityType.create(level);
        if (entity == null) {
            throw new MobException();
        }
        entity.moveTo(x, y, z, level.getRandom().nextFloat() * 360f, 0f);
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
        }
        if (!level.addFreshEntity(entity)) {
            throw new MobException();
        }
        return entity;
    }

    public EntityType<?> getType() {
        return entityType;
    }

    public enum Enemies {
        FRIENDLY("friendly"),
        NEUTRAL("neutral"),
        ENEMY("enemy"),
        ADULT_ENEMY("adult_enemy");

        final String type;

        Enemies(final String type) {
            this.type = type;
        }
    }

    public static class MobException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
