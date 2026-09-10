package net.essentialsx.fabric.items;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry-aware item database (Section 11.1) backed by the upstream {@code items.json}
 * alias table plus {@code custom_items.yml}. Namespaced ids (including modded ones) are
 * always accepted.
 */
public class ItemDb {
    private final Essentials ess;
    private final Map<String, ItemData> items = new HashMap<>();
    private final Map<String, String> itemAliases = new HashMap<>();
    private final Set<String> allAliases = new HashSet<>();
    private final Map<String, String> customAliases = new HashMap<>();
    private final Set<String> unknownNames = new HashSet<>();
    private final Set<String> registryNames = new HashSet<>();
    private boolean ready = false;

    public ItemDb(final Essentials ess) {
        this.ess = ess;
    }

    public boolean isReady() {
        return ready;
    }

    public void reloadConfig() {
        ready = false;
        items.clear();
        itemAliases.clear();
        allAliases.clear();
        customAliases.clear();
        unknownNames.clear();
        registryNames.clear();
        try (InputStream in = ItemDb.class.getResourceAsStream("/items.json")) {
            if (in != null) {
                final String json;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    json = reader.lines().filter(line -> !line.startsWith("#")).collect(Collectors.joining("\n"));
                }
                loadJSON(json);
            }
        } catch (final IOException e) {
            ess.getLogger().error("Failed to read items.json", e);
        }
        loadCustomItems();
        loadRegistryNames();
        ready = true;
        ess.getLogger().info("Loaded {} items from items.json and {} additional registry ids.", allAliases.size(), registryNames.size());
        if (!unknownNames.isEmpty()) {
            ess.getLogger().debug("{} item aliases reference items unknown to this server (e.g. newer content) and were skipped.", unknownNames.size());
        }
    }

    private void loadCustomItems() {
        final Path file = ess.getDataFolder().resolve("custom_items.yml");
        final YamlFile config = new YamlFile(file, "/custom_items.yml");
        config.load();
        final Map<String, Object> aliases = config.getSection("aliases");
        if (aliases == null) {
            return;
        }
        for (final Map.Entry<String, Object> entry : aliases.entrySet()) {
            if (entry.getValue() != null) {
                customAliases.put(entry.getKey().toLowerCase(Locale.ENGLISH), entry.getValue().toString());
                allAliases.add(entry.getKey().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    /** Namespaced ids of registered items that items.json does not know (modded and newer vanilla content). */
    private void loadRegistryNames() {
        final Set<Item> known = new HashSet<>();
        for (final ItemData data : items.values()) {
            known.add(data.item);
        }
        for (final Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getValue() != Items.AIR && !known.contains(entry.getValue())) {
                registryNames.add(entry.getKey().location().toString());
            }
        }
    }

    private void loadJSON(final String source) {
        final JsonObject map = JsonParser.parseString(source).getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : map.entrySet()) {
            final String key = entry.getKey();
            final JsonElement element = entry.getValue();
            boolean valid = false;
            if (element.isJsonObject()) {
                final ItemData data = ItemData.fromJson(element.getAsJsonObject());
                if (data != null) {
                    items.put(key, data);
                    valid = true;
                } else {
                    unknownNames.add(key);
                    continue;
                }
            } else {
                try {
                    final String target = element.getAsString();
                    itemAliases.put(key, target);
                    valid = true;
                } catch (final Exception ignored) {
                }
            }
            if (valid) {
                allAliases.add(key);
            }
        }
        // prune aliases whose target failed to resolve
        itemAliases.entrySet().removeIf(e -> !items.containsKey(e.getValue()));
        allAliases.removeIf(a -> !items.containsKey(a) && !itemAliases.containsKey(a));
    }

    public ItemStack get(final String id) throws Exception {
        return get(id, true);
    }

    public ItemStack get(final String id, final int quantity) throws Exception {
        final ItemStack stack = get(id, true);
        stack.setCount(quantity);
        return stack;
    }

    public ItemStack get(String id, final boolean useResolvers) throws Exception {
        id = id.toLowerCase(Locale.ENGLISH);
        if (useResolvers) {
            final String custom = customAliases.get(id);
            if (custom != null) {
                final String[] parts = custom.split(" +");
                final ItemStack base = get(parts[0], false);
                if (parts.length > 1) {
                    final MetaItemStack meta = new MetaItemStack(base);
                    meta.parseStringMeta(null, ess.getSettings().allowUnsafeEnchantments(), parts, 1, ess);
                    return meta.getItemStack();
                }
                return base;
            }
        }
        // Resolution order: the whole string as an Essentials name/alias or a namespaced registry id
        // (so modded ids such as "cobblemon:poke_ball" work), then the legacy "<item>:<durability>" form
        // where the suffix after the last colon is numeric and <item> may itself be namespaced.
        ItemData data = resolve(id);
        String durability = null;
        if (data == null) {
            final int colon = id.lastIndexOf(':');
            if (colon > 0 && colon < id.length() - 1 && NumberUtil.isInt(id.substring(colon + 1))) {
                durability = id.substring(colon + 1);
                data = resolve(id.substring(0, colon));
            }
        }
        if (data == null) {
            throw new TranslatableException("unknownItemName", id);
        }
        final Item item = data.item;
        if (item == Items.AIR) {
            throw new TranslatableException("unableToSpawnItem", id);
        }
        final ItemStack stack = new ItemStack(item);
        stack.setCount(item.getDefaultMaxStackSize());
        if (data.potion != null) {
            final Holder<Potion> potion = data.potion.resolve();
            if (potion != null) {
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            }
        }
        if (durability != null && stack.isDamageableItem()) {
            stack.setDamageValue(Integer.parseInt(durability));
        }
        if (data.entity != null && item == Items.SPAWNER) {
            final CompoundTag tag = new CompoundTag();
            final CompoundTag spawnData = new CompoundTag();
            final CompoundTag entityTag = new CompoundTag();
            entityTag.putString("id", EntityType.getKey(data.entity).toString());
            spawnData.put("entity", entityTag);
            tag.put("SpawnData", spawnData);
            tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.MOB_SPAWNER).toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        return stack;
    }

    /**
     * Resolve a single item id: Essentials names and aliases first (items.json + custom_items.yml
     * targets), then any id present in the item registry, vanilla or modded.
     */
    private ItemData resolve(final String name) {
        final ItemData data = getByName(name);
        if (data != null) {
            return data;
        }
        final ResourceLocation loc = ResourceLocation.tryParse(name);
        if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
            return new ItemData(BuiltInRegistries.ITEM.get(loc));
        }
        return null;
    }

    private ItemData getByName(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        if (items.containsKey(name)) {
            return items.get(name);
        } else if (itemAliases.containsKey(name)) {
            return items.get(itemAliases.get(name));
        }
        return null;
    }

    public List<String> nameList(final ItemStack item) {
        final List<String> names = new ArrayList<>();
        final String primaryName = name(item);
        names.add(primaryName);
        for (final Map.Entry<String, String> entry : itemAliases.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(primaryName)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    /**
     * Primary Essentials name of an item; namespaced id for items not in the table.
     */
    public String name(final ItemStack item) {
        final ItemData data = lookup(item);
        for (final Map.Entry<String, ItemData> entry : items.entrySet()) {
            if (entry.getValue().equals(data)) {
                return entry.getKey();
            }
        }
        return UserData.itemKey(item);
    }

    private ItemData lookup(final ItemStack is) {
        final Item type = is.getItem();
        if (MaterialUtil.isPotion(type)) {
            final PotionContents contents = is.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                return new ItemData(type, PotionRef.of(contents.potion().get()));
            }
        } else if (type == Items.SPAWNER) {
            final CustomData data = is.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data != null) {
                final CompoundTag tag = data.copyTag();
                if (tag.contains("SpawnData")) {
                    final CompoundTag entity = tag.getCompound("SpawnData").getCompound("entity");
                    if (entity.contains("id")) {
                        final ResourceLocation id = ResourceLocation.tryParse(entity.getString("id"));
                        if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                            return new ItemData(type, BuiltInRegistries.ENTITY_TYPE.get(id));
                        }
                    }
                }
            }
        }
        return new ItemData(type);
    }

    /**
     * Every name {@link #get(String)} accepts: Essentials names and aliases plus the namespaced id of
     * each registered item that has no Essentials name (modded items, or vanilla items newer than
     * the alias table). Used for tab completion.
     */
    public Collection<String> listNames() {
        final Set<String> names = new HashSet<>(allAliases);
        names.addAll(registryNames);
        return names;
    }

    public List<ItemStack> getMatching(final User user, final String[] args) throws Exception {
        final List<ItemStack> is = new ArrayList<>();
        if (args.length < 1) {
            is.add(user.getItemInHand().copy());
        } else if (args[0].equalsIgnoreCase("hand")) {
            is.add(user.getItemInHand().copy());
        } else if (args[0].equalsIgnoreCase("inventory") || args[0].equalsIgnoreCase("invent") || args[0].equalsIgnoreCase("all")) {
            for (final ItemStack stack : Inventories.getInventory(user.getBase(), true)) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                is.add(stack.copy());
            }
        } else if (args[0].equalsIgnoreCase("blocks")) {
            for (final ItemStack stack : Inventories.getInventory(user.getBase(), true)) {
                if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof net.minecraft.world.item.BlockItem)) {
                    continue;
                }
                is.add(stack.copy());
            }
        } else {
            is.add(get(args[0]));
        }
        if (is.isEmpty() || is.get(0).isEmpty()) {
            throw new Exception(user.playerTl("itemSellAir"));
        }
        return is;
    }

    /**
     * Serialize an item stack into the Essentials kit/string form.
     */
    public String serialize(final ItemStack is) {
        return serialize(is, true);
    }

    public String serialize(final ItemStack is, final boolean useResolvers) {
        final String mat = name(is);
        final int quantity = is.getCount();
        final StringBuilder sb = new StringBuilder();
        sb.append(mat).append(" ").append(quantity).append(" ");
        if (is.has(DataComponents.CUSTOM_NAME)) {
            sb.append("name:").append(FormatUtil.unformatString(net.essentialsx.fabric.text.Text.get().nativeToLegacy(is.get(DataComponents.CUSTOM_NAME))).replace(" ", "_")).append(" ");
        }
        final ItemLore lore = is.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            final List<String> lines = new ArrayList<>();
            for (final net.minecraft.network.chat.Component line : lore.lines()) {
                lines.add(net.essentialsx.fabric.text.Text.get().nativeToLegacy(line));
            }
            sb.append("lore:").append(serializeLines(lines)).append(" ");
        }
        if (is.has(DataComponents.CUSTOM_MODEL_DATA)) {
            sb.append("custom-model-data:").append(Objects.requireNonNull(is.get(DataComponents.CUSTOM_MODEL_DATA)).value()).append(" ");
        }
        if (is.has(DataComponents.UNBREAKABLE)) {
            sb.append("unbreakable:true ");
        }
        final ItemEnchantments enchants = is.getItem() == Items.ENCHANTED_BOOK ? is.get(DataComponents.STORED_ENCHANTMENTS) : is.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) {
            for (final Holder<Enchantment> e : enchants.keySet()) {
                sb.append(Enchantments.getRealName(e)).append(":").append(enchants.getLevel(e)).append(" ");
            }
        }
        if (MaterialUtil.isEditableBook(is.getItem())) {
            final net.minecraft.world.item.component.WrittenBookContent written = is.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (written != null) {
                if (!written.title().raw().isEmpty()) {
                    sb.append("title:").append(FormatUtil.unformatString(written.title().raw()).replace(' ', '_')).append(" ");
                }
                if (!written.author().isEmpty()) {
                    sb.append("author:").append(FormatUtil.unformatString(written.author()).replace(' ', '_')).append(" ");
                }
                final List<net.minecraft.server.network.Filterable<net.minecraft.network.chat.Component>> pages = written.pages();
                for (int i = 0; i < pages.size(); i++) {
                    sb.append("page").append(i + 1).append(":");
                    sb.append(serializeLines(List.of(net.essentialsx.fabric.text.Text.get().nativeToLegacy(pages.get(i).raw()).split("\n"))));
                    sb.append(" ");
                }
            }
            final net.minecraft.world.item.component.WritableBookContent writable = is.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writable != null) {
                final List<net.minecraft.server.network.Filterable<String>> pages = writable.pages();
                for (int i = 0; i < pages.size(); i++) {
                    sb.append("page").append(i + 1).append(":");
                    sb.append(serializeLines(List.of(pages.get(i).raw().split("\n"))));
                    sb.append(" ");
                }
            }
        }
        if (MaterialUtil.isPotion(is.getItem())) {
            final PotionContents contents = is.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                for (final net.minecraft.world.effect.MobEffectInstance e : contents.customEffects()) {
                    sb.append("effect:").append(Potions.getName(e.getEffect())).append(" ").append("power:").append(e.getAmplifier()).append(" ").append("duration:").append(e.getDuration() / 20).append(" ");
                }
            }
        } else if (MaterialUtil.isPlayerHead(is)) {
            final net.minecraft.world.item.component.ResolvableProfile profile = is.get(DataComponents.PROFILE);
            if (profile != null && profile.name().isPresent()) {
                sb.append("player:").append(profile.name().get()).append(" ");
            }
        } else if (MaterialUtil.isLeatherArmor(is.getItem())) {
            final net.minecraft.world.item.component.DyedItemColor color = is.get(DataComponents.DYED_COLOR);
            if (color != null) {
                sb.append("color:").append(color.rgb()).append(" ");
            }
        }
        if (MaterialUtil.isFirework(is.getItem())) {
            final net.minecraft.world.item.component.Fireworks fireworks = is.get(DataComponents.FIREWORKS);
            if (fireworks != null && !fireworks.explosions().isEmpty()) {
                for (final net.minecraft.world.item.component.FireworkExplosion effect : fireworks.explosions()) {
                    serializeEffectMeta(sb, effect);
                }
                sb.append("power:").append(fireworks.flightDuration()).append(" ");
            }
        } else if (MaterialUtil.isFireworkCharge(is.getItem())) {
            final net.minecraft.world.item.component.FireworkExplosion effect = is.get(DataComponents.FIREWORK_EXPLOSION);
            if (effect != null) {
                serializeEffectMeta(sb, effect);
            }
        }
        final net.minecraft.world.item.armortrim.ArmorTrim trim = is.get(DataComponents.TRIM);
        if (trim != null) {
            final String pattern = trim.pattern().unwrapKey().map(k -> k.location().getPath()).orElse("");
            final String material = trim.material().unwrapKey().map(k -> k.location().getPath()).orElse("");
            sb.append("trim:").append(pattern).append("|").append(material).append(" ");
        }
        return sb.toString().trim().replaceAll("§", "&");
    }

    private void serializeEffectMeta(final StringBuilder sb, final net.minecraft.world.item.component.FireworkExplosion effect) {
        if (!effect.colors().isEmpty()) {
            sb.append("color:");
            boolean first = true;
            for (int i = 0; i < effect.colors().size(); i++) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("#").append(Integer.toHexString(effect.colors().getInt(i)));
                first = false;
            }
            sb.append(" ");
        }
        sb.append("shape:").append(MetaItemStack.shapeName(effect.shape())).append(" ");
        if (!effect.fadeColors().isEmpty()) {
            sb.append("fade:");
            boolean first = true;
            for (int i = 0; i < effect.fadeColors().size(); i++) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("#").append(Integer.toHexString(effect.fadeColors().getInt(i)));
                first = false;
            }
            sb.append(" ");
        }
        if (effect.hasTwinkle() || effect.hasTrail()) {
            sb.append("effect:");
            if (effect.hasTwinkle()) {
                sb.append("twinkle");
            }
            if (effect.hasTrail()) {
                sb.append(effect.hasTwinkle() ? ",trail" : "trail");
            }
            sb.append(" ");
        }
    }

    private String serializeLines(final Iterable<String> lines) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String line : lines) {
            if (!first) {
                sb.append("|");
            }
            first = false;
            sb.append(FormatUtil.unformatString(line).replace(" ", "_").replace("|", "\\|"));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ data

    /**
     * Maps a Bukkit-style potion type name onto a Minecraft potion registry key.
     */
    static final class PotionRef {
        private static final Map<String, String> BUKKIT_TO_MC = Map.ofEntries(
            Map.entry("JUMP", "leaping"),
            Map.entry("SPEED", "swiftness"),
            Map.entry("INSTANT_HEAL", "healing"),
            Map.entry("INSTANT_DAMAGE", "harming"),
            Map.entry("REGEN", "regeneration"),
            Map.entry("UNCRAFTABLE", "mundane")
        );
        final String base;
        final boolean upgraded;
        final boolean extended;

        PotionRef(final String base, final boolean upgraded, final boolean extended) {
            this.base = base;
            this.upgraded = upgraded;
            this.extended = extended;
        }

        static PotionRef of(final Holder<Potion> holder) {
            final String path = holder.unwrapKey().map(k -> k.location().getPath()).orElse("water");
            if (path.startsWith("strong_")) {
                return new PotionRef(path.substring(7), true, false);
            }
            if (path.startsWith("long_")) {
                return new PotionRef(path.substring(5), false, true);
            }
            return new PotionRef(path, false, false);
        }

        static PotionRef fromBukkit(final String type, final boolean upgraded, final boolean extended) {
            String base = type.toUpperCase(Locale.ENGLISH);
            boolean up = upgraded, ext = extended;
            if (base.startsWith("STRONG_")) {
                base = base.substring(7);
                up = true;
            } else if (base.startsWith("LONG_")) {
                base = base.substring(5);
                ext = true;
            }
            final String mc = BUKKIT_TO_MC.getOrDefault(base, base.toLowerCase(Locale.ENGLISH));
            return new PotionRef(mc, up, ext);
        }

        Holder<Potion> resolve() {
            final String path = upgraded ? "strong_" + base : extended ? "long_" + base : base;
            final ResourceLocation loc = ResourceLocation.withDefaultNamespace(path);
            if (!BuiltInRegistries.POTION.containsKey(loc)) {
                return null;
            }
            return BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, loc)).orElse(null);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof PotionRef)) return false;
            final PotionRef that = (PotionRef) o;
            return upgraded == that.upgraded && extended == that.extended && base.equals(that.base);
        }

        @Override
        public int hashCode() {
            return Objects.hash(base, upgraded, extended);
        }
    }

    static final class ItemData {
        final Item item;
        PotionRef potion;
        EntityType<?> entity;

        ItemData(final Item item) {
            this.item = item;
        }

        ItemData(final Item item, final PotionRef potion) {
            this.item = item;
            this.potion = potion;
        }

        ItemData(final Item item, final EntityType<?> entity) {
            this.item = item;
            this.entity = entity;
        }

        static ItemData fromJson(final JsonObject obj) {
            Item item = null;
            if (obj.has("material")) {
                item = itemFromBukkit(obj.get("material").getAsString());
            }
            if (item == null && obj.has("fallbacks")) {
                for (final JsonElement fb : obj.getAsJsonArray("fallbacks")) {
                    item = itemFromBukkit(fb.getAsString());
                    if (item != null) {
                        break;
                    }
                }
            }
            if (item == null) {
                return null;
            }
            final ItemData data = new ItemData(item);
            if (obj.has("potionData")) {
                final JsonObject pd = obj.getAsJsonObject("potionData");
                final String type = pd.has("type") ? pd.get("type").getAsString() : pd.has("fallbackType") ? pd.get("fallbackType").getAsString() : "WATER";
                data.potion = PotionRef.fromBukkit(type, pd.has("upgraded") && pd.get("upgraded").getAsBoolean(), pd.has("extended") && pd.get("extended").getAsBoolean());
                if (data.potion.resolve() == null) {
                    return null;
                }
            }
            if (obj.has("entity")) {
                final ResourceLocation loc = ResourceLocation.withDefaultNamespace(entityFromBukkit(obj.get("entity").getAsString()));
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
                    data.entity = BuiltInRegistries.ENTITY_TYPE.get(loc);
                } else {
                    return null;
                }
            }
            return data;
        }

        private static Item itemFromBukkit(final String material) {
            final ResourceLocation loc = ResourceLocation.tryParse(material.toLowerCase(Locale.ENGLISH));
            if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) {
                return null;
            }
            return BuiltInRegistries.ITEM.get(loc);
        }

        private static String entityFromBukkit(final String name) {
            return switch (name.toUpperCase(Locale.ENGLISH)) {
                case "MUSHROOM_COW" -> "mooshroom";
                case "SNOWMAN" -> "snow_golem";
                case "PIG_ZOMBIE" -> "zombified_piglin";
                default -> name.toLowerCase(Locale.ENGLISH);
            };
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof ItemData)) {
                return false;
            }
            final ItemData that = (ItemData) o;
            return this.item == that.item && Objects.equals(potion, that.potion) && Objects.equals(entity, that.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, potion, entity);
        }
    }
}
