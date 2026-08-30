package net.essentialsx.fabric.items;

import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Mob data modifiers for {@code /spawnmob} ({@code sheep:red}, {@code zombie:baby,diamondsword}...).
 */
public final class MobData {
    private static final List<MobData> ALL = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        final List<String> dyes = Arrays.stream(DyeColor.values()).map(c -> c.getName().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
        add("baby", e -> e instanceof AgeableMob, (e, t, d) -> ((AgeableMob) e).setBaby(true), true);
        add("adult", e -> e instanceof AgeableMob, (e, t, d) -> ((AgeableMob) e).setBaby(false), true);
        add("baby", e -> e instanceof Zombie, (e, t, d) -> ((Zombie) e).setBaby(true), true);
        add("adult", e -> e instanceof Zombie, (e, t, d) -> ((Zombie) e).setBaby(false), true);
        for (final String[] alias : new String[][] {{"kitten", "cat"}, {"piglet", "pig"}, {"puppy", "wolf"}, {"chick", "chicken"}, {"colt", "horse"}, {"kitten", "ocelot"}, {"lamb", "sheep"}, {"calf", "cow"}, {"child", "villager"}}) {
            add(alias[0], type(alias[1]), (e, t, d) -> ((AgeableMob) e).setBaby(true), false);
        }
        add("tamed", e -> e instanceof TamableAnimal, (e, t, d) -> {
            ((TamableAnimal) e).setTame(true, true);
            ((TamableAnimal) e).setOwnerUUID(t.getUUID());
        }, true);
        add("tame", e -> e instanceof TamableAnimal, (e, t, d) -> {
            ((TamableAnimal) e).setTame(true, true);
            ((TamableAnimal) e).setOwnerUUID(t.getUUID());
        }, false);
        add("random", e -> e instanceof Sheep, (e, t, d) -> ((Sheep) e).setColor(DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)]), true);
        addMulti("", dyes, e -> e instanceof Sheep, (e, t, d) -> ((Sheep) e).setColor(parseDye(d)), true);
        add("random", e -> e instanceof Shulker, (e, t, d) -> ((Shulker) e).setVariant(java.util.Optional.of(DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)])), true);
        addMulti("", dyes, e -> e instanceof Shulker, (e, t, d) -> ((Shulker) e).setVariant(java.util.Optional.of(parseDye(d))), true);
        // horses
        final Object[][] horseStyles = {{"polka", Markings.BLACK_DOTS, true}, {"sooty", Markings.BLACK_DOTS, false}, {"blaze", Markings.WHITE, true}, {"socks", Markings.WHITE, false}, {"leopard", Markings.WHITE_DOTS, true}, {"appaloosa", Markings.WHITE_DOTS, false}, {"paint", Markings.WHITE_FIELD, true}, {"milky", Markings.WHITE_FIELD, false}, {"splotchy", Markings.WHITE_FIELD, false}};
        for (final Object[] row : horseStyles) {
            add((String) row[0], e -> e instanceof Horse, (e, t, d) -> setHorse((Horse) e, ((Horse) e).getVariant(), (Markings) row[1]), (Boolean) row[2]);
        }
        final Object[][] horseColors = {{"black", Variant.BLACK, true}, {"chestnut", Variant.CHESTNUT, true}, {"liver", Variant.CHESTNUT, false}, {"creamy", Variant.CREAMY, true}, {"flaxen", Variant.CREAMY, false}, {"gray", Variant.GRAY, true}, {"dapple", Variant.GRAY, false}, {"buckskin", Variant.DARK_BROWN, true}, {"darkbrown", Variant.DARK_BROWN, false}, {"dark", Variant.DARK_BROWN, false}, {"dbrown", Variant.DARK_BROWN, false}, {"bay", Variant.BROWN, true}, {"brown", Variant.BROWN, false}, {"white", Variant.WHITE, true}};
        for (final Object[] row : horseColors) {
            add((String) row[0], e -> e instanceof Horse, (e, t, d) -> setHorse((Horse) e, (Variant) row[1], ((Horse) e).getMarkings()), (Boolean) row[2]);
        }
        add("saddle", e -> e instanceof AbstractHorse, (e, t, d) -> {
            final AbstractHorse horse = (AbstractHorse) e;
            horse.setTamed(true);
            horse.setOwnerUUID(t.getUUID());
            horse.equipSaddle(new ItemStack(Items.SADDLE), null);
        }, true);
        add("chest", e -> e instanceof net.minecraft.world.entity.animal.horse.AbstractChestedHorse, (e, t, d) -> ((net.minecraft.world.entity.animal.horse.AbstractChestedHorse) e).setChest(true), true);
        for (final String[] armor : new String[][] {{"goldarmor", "golden_horse_armor"}, {"diamondarmor", "diamond_horse_armor"}, {"netheritearmor", "netherite_horse_armor"}, {"armor", "iron_horse_armor"}, {"leatherarmor", "leather_horse_armor"}}) {
            add(armor[0], e -> e instanceof Horse, (e, t, d) -> ((Horse) e).setBodyArmorItem(new ItemStack(item(armor[1]))), true);
        }
        // cats
        final String[][] cats = {{"siamese", "siamese", "true"}, {"white", "white", "false"}, {"red", "red", "true"}, {"orange", "red", "false"}, {"tabby", "tabby", "true"}, {"black", "black", "true"}, {"tuxedo", "black", "true"}, {"britishshorthair", "british_shorthair", "true"}, {"calico", "calico", "true"}, {"persian", "persian", "true"}, {"ragdoll", "ragdoll", "true"}, {"jellie", "jellie", "true"}, {"allblack", "all_black", "true"}};
        for (final String[] cat : cats) {
            add(cat[0], e -> e instanceof Cat, (e, t, d) -> catVariant((Cat) e, cat[1]), Boolean.parseBoolean(cat[2]));
        }
        // zombies / skeletons weapons & armor
        for (final String[] sword : new String[][] {{"netheritesword", "netherite_sword", "true"}, {"diamondsword", "diamond_sword", "true"}, {"goldsword", "golden_sword", "true"}, {"ironsword", "iron_sword", "true"}, {"stonesword", "stone_sword", "false"}, {"sword", "stone_sword", "true"}}) {
            add(sword[0], e -> e instanceof Zombie || e.getType() == EntityType.SKELETON, (e, t, d) -> ((LivingEntity) e).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item(sword[1]))), Boolean.parseBoolean(sword[2]));
        }
        add("bow", e -> e.getType() == EntityType.SKELETON, (e, t, d) -> ((LivingEntity) e).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)), true);
        add("powered", e -> e instanceof Creeper, (e, t, d) -> nbt(e, tag -> tag.putBoolean("powered", true)), true);
        add("electric", e -> e instanceof Creeper, (e, t, d) -> nbt(e, tag -> tag.putBoolean("powered", true)), false);
        add("charged", e -> e instanceof Creeper, (e, t, d) -> nbt(e, tag -> tag.putBoolean("powered", true)), false);
        add("saddle", e -> e instanceof Pig, (e, t, d) -> ((Pig) e).equipSaddle(new ItemStack(Items.SADDLE), null), true);
        add("angry", e -> e instanceof Wolf, (e, t, d) -> ((Wolf) e).setRemainingPersistentAngerTime(Integer.MAX_VALUE), true);
        add("rabid", e -> e instanceof Wolf, (e, t, d) -> ((Wolf) e).setRemainingPersistentAngerTime(Integer.MAX_VALUE), false);
        // villagers
        for (final String prof : new String[] {"none", "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith"}) {
            add(prof.equals("none") ? "villager" : prof, e -> e instanceof Villager, (e, t, d) -> {
                final Villager v = (Villager) e;
                final VillagerProfession p = BuiltInRegistries.VILLAGER_PROFESSION.get(ResourceLocation.withDefaultNamespace(prof));
                v.setVillagerData(v.getVillagerData().setProfession(p));
            }, true);
        }
        for (final String vt : new String[] {"desert", "jungle", "plains", "savanna", "snowy", "swamp", "taiga"}) {
            add(vt, e -> e instanceof Villager, (e, t, d) -> {
                final Villager v = (Villager) e;
                final VillagerType type = BuiltInRegistries.VILLAGER_TYPE.get(ResourceLocation.withDefaultNamespace(vt));
                v.setVillagerData(v.getVillagerData().setType(type));
            }, true);
        }
        addMulti("", Collections.singletonList("<1-100>"), e -> e instanceof Slime, (e, t, d) -> ((Slime) e).setSize(clamp(parseInt(d), 1, 100), true), true);
        addMulti("", Collections.singletonList("<1-100>"), e -> e instanceof Phantom, (e, t, d) -> ((Phantom) e).setPhantomSize(clamp(parseInt(d), 1, 100)), true);
        addMulti("", Collections.singletonList("<1-2000000000>"), e -> e instanceof ExperienceOrb, (e, t, d) -> nbt(e, tag -> tag.putShort("Value", (short) Math.min(Short.MAX_VALUE, Math.max(1, parseInt(d))))), true);
        for (final String[] parrot : new String[][] {{"red", "RED_BLUE"}, {"green", "GREEN"}, {"blue", "BLUE"}, {"cyan", "YELLOW_BLUE"}, {"gray", "GRAY"}}) {
            add(parrot[0], e -> e instanceof Parrot, (e, t, d) -> ((Parrot) e).setVariant(Parrot.Variant.valueOf(parrot[1])), true);
        }
        for (final TropicalFish.Pattern pattern : TropicalFish.Pattern.values()) {
            add(pattern.name().toLowerCase(Locale.ENGLISH), e -> e instanceof TropicalFish, (e, t, d) -> ((TropicalFish) e).setVariant(pattern), true);
        }
        add("brown", e -> e instanceof MushroomCow, (e, t, d) -> ((MushroomCow) e).setVariant(MushroomCow.MushroomType.BROWN), true);
        add("red", e -> e instanceof MushroomCow, (e, t, d) -> ((MushroomCow) e).setVariant(MushroomCow.MushroomType.RED), true);
        for (final Panda.Gene gene : Panda.Gene.values()) {
            add(gene.getSerializedName(), e -> e instanceof Panda, (e, t, d) -> ((Panda) e).setMainGene(gene), true);
            add(gene.getSerializedName() + "_hidden", e -> e instanceof Panda, (e, t, d) -> ((Panda) e).setHiddenGene(gene), true);
        }
        for (final Llama.Variant variant : Llama.Variant.values()) {
            add(variant.getSerializedName(), e -> e instanceof Llama, (e, t, d) -> ((Llama) e).setVariant(variant), true);
        }
        add("red", e -> e instanceof Fox, (e, t, d) -> ((Fox) e).setVariant(Fox.Type.RED), true);
        add("snow", e -> e instanceof Fox, (e, t, d) -> ((Fox) e).setVariant(Fox.Type.SNOW), true);
        add("leader", e -> e instanceof Raider, (e, t, d) -> ((Raider) e).setPatrolLeader(true), true);
        for (final Axolotl.Variant variant : Axolotl.Variant.values()) {
            add(variant.getName(), e -> e instanceof Axolotl, (e, t, d) -> ((Axolotl) e).setVariant(variant), true);
        }
        add("screaming", e -> e instanceof Goat, (e, t, d) -> ((Goat) e).setScreamingGoat(true), true);
        for (final String[] frog : new String[][] {{"temperate", "temperate"}, {"warm", "warm"}, {"cold", "cold"}}) {
            add(frog[0], e -> e instanceof Frog, (e, t, d) -> {
                final Holder<net.minecraft.world.entity.animal.FrogVariant> holder = BuiltInRegistries.FROG_VARIANT.getHolder(ResourceKey.create(Registries.FROG_VARIANT, ResourceLocation.withDefaultNamespace(frog[1]))).orElse(null);
                if (holder != null) {
                    ((Frog) e).setVariant(holder);
                }
            }, true);
        }
        for (final Boat.Type type : Boat.Type.values()) {
            add(type.getName().replace("_", ""), e -> e instanceof Boat, (e, t, d) -> ((Boat) e).setVariant(type), true);
        }
        for (final String[] wolf : new String[][] {{"pale", "pale"}, {"spotted", "spotted"}, {"snowy", "snowy"}, {"black", "black"}, {"ashen", "ashen"}, {"rusty", "rusty"}, {"woods", "woods"}, {"chestnut", "chestnut"}, {"striped", "striped"}}) {
            add(wolf[0], e -> e instanceof Wolf, (e, t, d) -> {
                final Holder<net.minecraft.world.entity.animal.WolfVariant> holder = e.level().registryAccess().registryOrThrow(Registries.WOLF_VARIANT).getHolder(ResourceKey.create(Registries.WOLF_VARIANT, ResourceLocation.withDefaultNamespace(wolf[1]))).orElse(null);
                if (holder != null) {
                    ((Wolf) e).setVariant(holder);
                }
            }, true);
        }
    }

    private static void nbt(final Entity entity, final Consumer<net.minecraft.nbt.CompoundTag> edit) {
        final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        entity.saveWithoutId(tag);
        edit.accept(tag);
        entity.load(tag);
    }

    private static void setHorse(final Horse horse, final Variant variant, final Markings markings) {
        // Horse#setVariantAndMarkings is private; the packed "Variant" NBT int is variant | markings << 8.
        nbt(horse, tag -> tag.putInt("Variant", variant.getId() & 0xFF | markings.getId() << 8));
    }

    private static Predicate<Entity> type(final String key) {
        final ResourceLocation loc = ResourceLocation.withDefaultNamespace(key);
        return e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(loc);
    }

    private static Item item(final String key) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(key));
    }

    private static DyeColor parseDye(final String data) throws TranslatableException {
        for (final DyeColor color : DyeColor.values()) {
            if (data.contains(color.getName().toLowerCase(Locale.ENGLISH))) {
                return color;
            }
        }
        throw new TranslatableException("sheepMalformedColor");
    }

    private static void catVariant(final Cat cat, final String key) {
        BuiltInRegistries.CAT_VARIANT.getHolder(ResourceKey.create(Registries.CAT_VARIANT, ResourceLocation.withDefaultNamespace(key))).ifPresent(cat::setVariant);
    }

    private static int parseInt(final String data) {
        final java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(data);
        if (m.find()) {
            return Integer.parseInt(m.group());
        }
        throw new NumberFormatException(data);
    }

    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }

    @FunctionalInterface
    private interface Applier {
        void apply(Entity entity, ServerPlayer target, String data) throws Exception;
    }

    private static void add(final String nickname, final Predicate<Entity> applies, final Applier applier, final boolean isPublic) {
        ALL.add(new MobData(nickname, Collections.singletonList(nickname), applies, applier, isPublic));
    }

    private static void addMulti(final String nickname, final List<String> suggestions, final Predicate<Entity> applies, final Applier applier, final boolean isPublic) {
        ALL.add(new MobData(nickname, suggestions, applies, applier, isPublic));
    }

    private final String nickname;
    private final List<String> suggestions;
    private final Predicate<Entity> applies;
    private final Applier applier;
    private final boolean isPublic;
    private String matched;

    private MobData(final String nickname, final List<String> suggestions, final Predicate<Entity> applies, final Applier applier, final boolean isPublic) {
        this.nickname = nickname;
        this.matched = nickname;
        this.suggestions = suggestions;
        this.applies = applies;
        this.applier = applier;
        this.isPublic = isPublic;
    }

    public static LinkedHashMap<String, MobData> getPossibleData(final Entity spawned, final boolean publicOnly) {
        final LinkedHashMap<String, MobData> mobList = new LinkedHashMap<>();
        for (final MobData data : ALL) {
            if (publicOnly && !data.isPublic) {
                continue;
            }
            if (data.applies.test(spawned)) {
                mobList.putIfAbsent(data.nickname.toLowerCase(Locale.ENGLISH) + "#" + System.identityHashCode(data), data);
            }
        }
        return mobList;
    }

    public static List<String> getValidHelp(final Entity spawned) {
        final List<String> output = new ArrayList<>();
        for (final MobData data : getPossibleData(spawned, true).values()) {
            output.add(StringUtil.joinList(data.suggestions));
        }
        return output;
    }

    public static MobData fromData(final Entity spawned, final String name) {
        if (name.isEmpty()) {
            return null;
        }
        for (final MobData data : getPossibleData(spawned, false).values()) {
            for (final String suggestion : data.suggestions) {
                if (suggestion.startsWith("<")) {
                    if (name.matches(".*\\d+.*")) {
                        final MobData copy = new MobData(data.nickname, data.suggestions, data.applies, data.applier, data.isPublic);
                        copy.matched = name.replaceAll("[^\\d]", "");
                        return copy;
                    }
                    continue;
                }
                if (!suggestion.isEmpty() && name.contains(suggestion)) {
                    final MobData copy = new MobData(data.nickname, data.suggestions, data.applies, data.applier, data.isPublic);
                    copy.matched = suggestion;
                    return copy;
                }
            }
        }
        return null;
    }

    public String getMatched() {
        return this.matched;
    }

    public void setData(final Entity spawned, final ServerPlayer target, final String rawData) throws Exception {
        applier.apply(spawned, target, rawData);
    }
}
