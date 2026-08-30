package net.essentialsx.fabric.items;

import com.mojang.authlib.GameProfile;
import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.textreader.BookInput;
import net.essentialsx.fabric.textreader.BookPager;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BannerPattern;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parses the Essentials item meta syntax ({@code name:... lore:... sharpness:5 ...}) onto a
 * 1.21.1 {@link ItemStack} using data components (Section 11.1).
 */
public class MetaItemStack {
    private static final Map<String, Integer> colorMap = new HashMap<>();
    private static final Map<String, FireworkExplosion.Shape> fireworkShape = new HashMap<>();
    private static final Pattern splitPattern = Pattern.compile("[:+',;.]");
    private static final Pattern hexPattern = Pattern.compile("#([0-9a-fA-F]{6})");

    static {
        for (final DyeColor color : DyeColor.values()) {
            colorMap.put(color.name(), color.getFireworkColor());
        }
        for (final FireworkExplosion.Shape shape : FireworkExplosion.Shape.values()) {
            fireworkShape.put(shape.name(), shape);
            fireworkShape.put(shapeName(shape), shape);
        }
    }

    private ItemStack stack;
    private FireworkBuilder builder = new FireworkBuilder();
    private Holder<MobEffect> pEffectType;
    private MobEffectInstance pEffect;
    private boolean validFirework = false;
    private boolean validFireworkCharge = false;
    private boolean validPotionEffect = false;
    private boolean validPotionDuration = false;
    private boolean validPotionPower = false;
    private boolean isSplashPotion = false;
    private boolean completePotion = false;
    private int power = 1;
    private int duration = 120;

    public MetaItemStack(final ItemStack stack) {
        this.stack = stack.copy();
    }

    /** Bukkit-compatible shape name (BALL, BALL_LARGE, STAR, CREEPER, BURST). */
    public static String shapeName(final FireworkExplosion.Shape shape) {
        return switch (shape) {
            case SMALL_BALL -> "BALL";
            case LARGE_BALL -> "BALL_LARGE";
            case STAR -> "STAR";
            case CREEPER -> "CREEPER";
            case BURST -> "BURST";
        };
    }

    public ItemStack getItemStack() {
        return stack;
    }

    public boolean isValidFirework() {
        return validFirework;
    }

    public boolean isValidPotion() {
        return validPotionEffect && validPotionDuration && validPotionPower;
    }

    public FireworkBuilder getFireworkBuilder() {
        return builder;
    }

    public MobEffectInstance getPotionEffect() {
        return pEffect;
    }

    public boolean completePotion() {
        return completePotion;
    }

    private void resetPotionMeta() {
        pEffect = null;
        pEffectType = null;
        validPotionEffect = false;
        validPotionDuration = false;
        validPotionPower = false;
        isSplashPotion = false;
        completePotion = true;
    }

    public boolean canSpawn(final Essentials ess) {
        return !stack.isEmpty();
    }

    public void parseStringMeta(final CommandSource sender, final boolean allowUnsafe, final String[] string, final int fromArg, final Essentials ess) throws Exception {
        if (string[fromArg].startsWith("{") && hasMetaPermission(sender, "vanilla", false, true, ess)) {
            throw new TranslatableException("noMetaNbtKill");
        } else if (string[fromArg].startsWith("[") && hasMetaPermission(sender, "vanilla", false, true, ess)) {
            final String components = String.join(" ", Arrays.asList(string).subList(fromArg, string.length));
            try {
                final String itemString = BuiltInRegistries.ITEM.getKey(stack.getItem()) + components;
                final net.minecraft.commands.arguments.item.ItemParser parser = new net.minecraft.commands.arguments.item.ItemParser(ess.getServer().registryAccess());
                final net.minecraft.commands.arguments.item.ItemParser.ItemResult result = parser.parse(new com.mojang.brigadier.StringReader(itemString));
                final ItemStack parsed = new ItemStack(result.item(), stack.getCount());
                parsed.applyComponents(result.components());
                stack = parsed;
            } catch (final Throwable throwable) {
                throw new Exception(throwable.getMessage(), throwable);
            }
        } else {
            for (int i = fromArg; i < string.length; i++) {
                addStringMeta(sender, allowUnsafe, string[i], ess);
            }
            if (validFirework) {
                if (!hasMetaPermission(sender, "firework", true, true, ess)) {
                    throw new TranslatableException("noMetaFirework");
                }
                final Fireworks fireworks = stack.getOrDefault(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
                final List<FireworkExplosion> explosions = new ArrayList<>(fireworks.explosions());
                explosions.add(builder.build());
                if (explosions.size() > 1 && !hasMetaPermission(sender, "firework-multiple", true, true, ess)) {
                    throw new TranslatableException("multipleCharges");
                }
                stack.set(DataComponents.FIREWORKS, new Fireworks(fireworks.flightDuration(), explosions));
            }
            if (validFireworkCharge) {
                if (!hasMetaPermission(sender, "firework", true, true, ess)) {
                    throw new TranslatableException("noMetaFirework");
                }
                stack.set(DataComponents.FIREWORK_EXPLOSION, builder.build());
            }
        }
    }

    public void addStringMeta(final CommandSource sender, final boolean allowUnsafe, final String string, final Essentials ess) throws Exception {
        final String[] split = splitPattern.split(string, 2);
        if (split.length < 1) {
            return;
        }
        if (split.length > 1 && split[0].equalsIgnoreCase("name") && hasMetaPermission(sender, "name", false, true, ess)) {
            final String displayName = FormatUtil.replaceFormat(split[1].replaceAll("(?<!\\\\)_", " ").replace("\\_", "_"));
            stack.set(DataComponents.CUSTOM_NAME, Text.get().legacy(displayName));
        } else if (split.length > 1 && (split[0].equalsIgnoreCase("lore") || split[0].equalsIgnoreCase("desc")) && hasMetaPermission(sender, "lore", false, true, ess)) {
            final List<Component> lore = new ArrayList<>();
            for (final String line : split[1].split("(?<!\\\\)\\|")) {
                lore.add(Text.get().legacy(FormatUtil.replaceFormat(line.replaceAll("(?<!\\\\)_", " ").replace("\\_", "_").replace("\\|", "|"))));
            }
            stack.set(DataComponents.LORE, new ItemLore(lore));
        } else if ((split[0].equalsIgnoreCase("custom-model-data") || split[0].equalsIgnoreCase("cmd")) && hasMetaPermission(sender, "custom-model-data", false, true, ess)) {
            final int value = split.length <= 1 ? 0 : Integer.parseInt(split[1]);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
        } else if (split[0].equalsIgnoreCase("unbreakable") && hasMetaPermission(sender, "unbreakable", false, true, ess)) {
            final boolean value = split.length <= 1 || Boolean.parseBoolean(split[1]);
            setUnbreakable(stack, value);
        } else if (split.length > 1 && (split[0].equalsIgnoreCase("player") || split[0].equalsIgnoreCase("owner")) && hasMetaPermission(sender, "head", false, true, ess)) {
            if (MaterialUtil.isPlayerHead(stack)) {
                setSkullOwner(ess, stack, split[1]);
            } else {
                throw new TranslatableException("onlyPlayerSkulls");
            }
        } else if (split.length > 1 && split[0].equalsIgnoreCase("book") && MaterialUtil.isEditableBook(stack.getItem()) && (hasMetaPermission(sender, "book", true, true, ess) || hasMetaPermission(sender, "chapter-" + split[1].toLowerCase(Locale.ENGLISH), true, true, ess))) {
            final IText input = new BookInput("book", true, ess);
            final BookPager pager = new BookPager(input);
            final List<String> pages = pager.getPages(split[1]);
            if (stack.getItem() == Items.WRITTEN_BOOK) {
                final WrittenBookContent existing = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
                String author = existing.author();
                if (author.isEmpty()) {
                    author = sender == null || !sender.isPlayer() ? Console.getInstance().getDisplayName() : sender.getPlayer().getGameProfile().getName();
                }
                String title = existing.title().raw();
                if (title.isEmpty()) {
                    title = FormatUtil.replaceFormat(split[1].replace('_', ' '));
                    title = title.length() > 32 ? title.substring(0, 32) : title;
                }
                final List<Filterable<Component>> components = new ArrayList<>();
                for (final String page : pages) {
                    components.add(Filterable.passThrough(Text.get().legacy(page)));
                }
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, existing.generation(), components, existing.resolved()));
            } else {
                final List<Filterable<String>> components = new ArrayList<>();
                for (final String page : pages) {
                    components.add(Filterable.passThrough(page));
                }
                stack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(components));
            }
        } else if (split.length > 1 && split[0].equalsIgnoreCase("author") && stack.getItem() == Items.WRITTEN_BOOK && hasMetaPermission(sender, "author", false, true, ess)) {
            final String author = FormatUtil.replaceFormat(split[1]);
            final WrittenBookContent existing = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(existing.title(), author, existing.generation(), existing.pages(), existing.resolved()));
        } else if (split.length > 1 && split[0].equalsIgnoreCase("title") && stack.getItem() == Items.WRITTEN_BOOK && hasMetaPermission(sender, "title", false, true, ess)) {
            final String title = FormatUtil.replaceFormat(split[1].replaceAll("(?<!\\\\)_", " ").replace("\\_", "_"));
            final WrittenBookContent existing = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title.length() > 32 ? title.substring(0, 32) : title), existing.author(), existing.generation(), existing.pages(), existing.resolved()));
        } else if (split.length > 1 && split[0].startsWith("page") && split[0].length() > 4
            && MaterialUtil.isEditableBook(stack.getItem())
            && hasMetaPermission(sender, "page", false, true, ess)) {
            final int page = NumberUtil.isInt(split[0].substring(4)) ? (Integer.parseInt(split[0].substring(4)) - 1) : 0;
            if (page > 100) {
                throw new TranslatableException("pageLimitExceeded");
            }
            final List<String> lines = new ArrayList<>();
            for (final String line : split[1].split("(?<!\\\\)\\|")) {
                lines.add(FormatUtil.replaceFormat(line.replaceAll("(?<!\\\\)_", " ").replace("\\_", "_").replace("\\|", "|")));
            }
            final String content = String.join("\n", lines);
            if (stack.getItem() == Items.WRITTEN_BOOK) {
                final WrittenBookContent existing = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
                final List<Filterable<Component>> pages = new ArrayList<>(existing.pages());
                while (pages.size() <= page) {
                    pages.add(Filterable.passThrough(Component.empty()));
                }
                pages.set(page, Filterable.passThrough(Text.get().legacy(content)));
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(existing.title(), existing.author(), existing.generation(), pages, existing.resolved()));
            } else {
                final WritableBookContent existing = stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
                final List<Filterable<String>> pages = new ArrayList<>(existing.pages());
                while (pages.size() <= page) {
                    pages.add(Filterable.passThrough(""));
                }
                pages.set(page, Filterable.passThrough(content));
                stack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
            }
        } else if (split.length > 1 && split[0].equalsIgnoreCase("power") && MaterialUtil.isFirework(stack.getItem()) && hasMetaPermission(sender, "firework-power", false, true, ess)) {
            final int power = NumberUtil.isInt(split[1]) ? Integer.parseInt(split[1]) : 0;
            final Fireworks fireworks = stack.getOrDefault(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
            stack.set(DataComponents.FIREWORKS, new Fireworks(power > 3 ? 4 : Math.max(0, power), fireworks.explosions()));
        } else if (split.length > 1 && split[0].equalsIgnoreCase("itemflags") && hasMetaPermission(sender, "itemflags", false, true, ess)) {
            addItemFlags(string);
        } else if (MaterialUtil.isFirework(stack.getItem())) {
            if (!parseEnchantmentStrings(sender, allowUnsafe, split, ess)) {
                addFireworkMeta(sender, false, string, ess);
            }
        } else if (MaterialUtil.isFireworkCharge(stack.getItem())) {
            if (!parseEnchantmentStrings(sender, allowUnsafe, split, ess)) {
                addChargeMeta(sender, false, string, ess);
            }
        } else if (MaterialUtil.isPotion(stack.getItem())) {
            if (split[0].equalsIgnoreCase("power") || !parseEnchantmentStrings(sender, allowUnsafe, split, ess)) {
                addPotionMeta(sender, false, string, ess);
            }
        } else if (MaterialUtil.isBanner(stack.getItem())) {
            if (!parseEnchantmentStrings(sender, allowUnsafe, split, ess)) {
                addBannerMeta(sender, false, string, ess);
            }
        } else if (split.length > 1 && (split[0].equalsIgnoreCase("color") || split[0].equalsIgnoreCase("colour")) && MaterialUtil.isLeatherArmor(stack.getItem())) {
            final String[] color = split[1].split("[|,]");
            if (color.length == 1 && (NumberUtil.isInt(color[0]) || color[0].startsWith("#"))) {
                final String input = color[0];
                final int rgb;
                if (input.startsWith("#")) {
                    rgb = Integer.valueOf(input.substring(1, 3), 16) << 16 | Integer.valueOf(input.substring(3, 5), 16) << 8 | Integer.valueOf(input.substring(5, 7), 16);
                } else {
                    rgb = Integer.parseInt(input);
                }
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, true));
            } else if (color.length == 3) {
                final int red = NumberUtil.isInt(color[0]) ? Integer.parseInt(color[0]) : 0;
                final int green = NumberUtil.isInt(color[1]) ? Integer.parseInt(color[1]) : 0;
                final int blue = NumberUtil.isInt(color[2]) ? Integer.parseInt(color[2]) : 0;
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor((red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF), true));
            } else {
                throw new TranslatableException("leatherSyntax");
            }
        } else if (MaterialUtil.isArmor(stack.getItem()) && split.length > 1 && split[0].equalsIgnoreCase("trim")) {
            final String[] trimData = split[1].split("\\|");
            if (trimData.length < 2) {
                throw new TranslatableException("invalidItemFlagMeta", string);
            }
            final var registries = ess.getServer().registryAccess();
            final Optional<Holder.Reference<TrimPattern>> pattern = registries.lookupOrThrow(Registries.TRIM_PATTERN).get(ResourceKey.create(Registries.TRIM_PATTERN, ResourceLocation.withDefaultNamespace(trimData[0].toLowerCase(Locale.ENGLISH))));
            final Optional<Holder.Reference<TrimMaterial>> material = registries.lookupOrThrow(Registries.TRIM_MATERIAL).get(ResourceKey.create(Registries.TRIM_MATERIAL, ResourceLocation.withDefaultNamespace(trimData[1].toLowerCase(Locale.ENGLISH))));
            if (pattern.isEmpty() || material.isEmpty()) {
                throw new TranslatableException("invalidItemFlagMeta", string);
            }
            stack.set(DataComponents.TRIM, new ArmorTrim(material.get(), pattern.get()));
        } else {
            parseEnchantmentStrings(sender, allowUnsafe, split, ess);
        }
    }

    public void addItemFlags(final String string) throws Exception {
        final String[] separate = splitPattern.split(string, 2);
        if (separate.length != 2) {
            throw new TranslatableException("invalidItemFlagMeta", string);
        }
        final String[] split = separate[1].split(",");
        boolean any = false;
        for (final String s : split) {
            switch (s.toUpperCase(Locale.ENGLISH)) {
                case "HIDE_ENCHANTS" -> {
                    final ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                    stack.set(DataComponents.ENCHANTMENTS, ench.withTooltip(false));
                    any = true;
                }
                case "HIDE_ATTRIBUTES" -> {
                    final var attrs = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
                    stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs.withTooltip(false));
                    any = true;
                }
                case "HIDE_UNBREAKABLE" -> {
                    if (stack.has(DataComponents.UNBREAKABLE)) {
                        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
                        any = true;
                    }
                }
                case "HIDE_DYE" -> {
                    final DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
                    if (color != null) {
                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.rgb(), false));
                        any = true;
                    }
                }
                case "HIDE_ARMOR_TRIM" -> {
                    final ArmorTrim trim = stack.get(DataComponents.TRIM);
                    if (trim != null) {
                        stack.set(DataComponents.TRIM, trim.withTooltip(false));
                        any = true;
                    }
                }
                case "HIDE_ADDITIONAL_TOOLTIP", "HIDE_POTION_EFFECTS", "HIDE_DESTROYS", "HIDE_PLACED_ON", "HIDE_STORED_ENCHANTS" -> {
                    stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
                    any = true;
                }
                default -> {
                }
            }
        }
        if (!any) {
            throw new TranslatableException("invalidItemFlagMeta", string);
        }
    }

    private static void setSkullOwner(final Essentials ess, final ItemStack stack, final String owner) {
        final java.util.UUID uuid = ess.getUsers().getUuid(owner);
        final GameProfile profile = uuid != null ? new GameProfile(uuid, owner) : new GameProfile(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + owner).getBytes(java.nio.charset.StandardCharsets.UTF_8)), owner);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        // Resolve textures asynchronously; the component updates when the profile completes.
        net.minecraft.world.item.component.ResolvableProfile resolvable = stack.get(DataComponents.PROFILE);
        if (resolvable != null && !resolvable.isResolved()) {
            resolvable.resolve().thenAcceptAsync(resolved -> stack.set(DataComponents.PROFILE, resolved), ess.getServer());
        }
    }

    private void addChargeMeta(final CommandSource sender, final boolean allowShortName, final String string, final Essentials ess) throws Exception {
        final String[] split = splitPattern.split(string, 2);
        if (split.length < 2) {
            return;
        }
        if (split[0].equalsIgnoreCase("color") || split[0].equalsIgnoreCase("colour") || (allowShortName && split[0].equalsIgnoreCase("c"))) {
            final IntList primaryColors = new IntArrayList();
            for (final String color : split[1].split(",")) {
                if (colorMap.containsKey(color.toUpperCase())) {
                    validFireworkCharge = true;
                    primaryColors.add(colorMap.get(color.toUpperCase()).intValue());
                } else if (hexPattern.matcher(color).matches()) {
                    validFireworkCharge = true;
                    primaryColors.add(Integer.decode(color).intValue());
                } else {
                    throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                }
            }
            builder.colors = primaryColors;
        } else if (split[0].equalsIgnoreCase("shape") || split[0].equalsIgnoreCase("type") || (allowShortName && (split[0].equalsIgnoreCase("s") || split[0].equalsIgnoreCase("t")))) {
            final String shape = split[1].equalsIgnoreCase("large") ? "BALL_LARGE" : split[1];
            if (fireworkShape.containsKey(shape.toUpperCase())) {
                builder.shape = fireworkShape.get(shape.toUpperCase());
            } else {
                throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
            }
        } else if (split[0].equalsIgnoreCase("fade") || (allowShortName && split[0].equalsIgnoreCase("f"))) {
            final IntList fadeColors = new IntArrayList();
            for (final String color : split[1].split(",")) {
                if (colorMap.containsKey(color.toUpperCase())) {
                    fadeColors.add(colorMap.get(color.toUpperCase()).intValue());
                } else if (hexPattern.matcher(color).matches()) {
                    fadeColors.add(Integer.decode(color).intValue());
                } else {
                    throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                }
            }
            if (!fadeColors.isEmpty()) {
                builder.fade = fadeColors;
            }
        } else if (split[0].equalsIgnoreCase("effect") || (allowShortName && split[0].equalsIgnoreCase("e"))) {
            for (final String effect : split[1].split(",")) {
                if (effect.equalsIgnoreCase("twinkle")) {
                    builder.flicker = true;
                } else if (effect.equalsIgnoreCase("trail")) {
                    builder.trail = true;
                } else {
                    throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                }
            }
        }
    }

    public void addFireworkMeta(final CommandSource sender, final boolean allowShortName, final String string, final Essentials ess) throws Exception {
        if (MaterialUtil.isFirework(stack.getItem())) {
            final String[] split = splitPattern.split(string, 2);
            if (split.length < 2) {
                return;
            }
            if (split[0].equalsIgnoreCase("color") || split[0].equalsIgnoreCase("colour") || (allowShortName && split[0].equalsIgnoreCase("c"))) {
                if (validFirework) {
                    if (!hasMetaPermission(sender, "firework", true, true, ess)) {
                        throw new TranslatableException("noMetaFirework");
                    }
                    final Fireworks fireworks = stack.getOrDefault(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
                    final List<FireworkExplosion> explosions = new ArrayList<>(fireworks.explosions());
                    explosions.add(builder.build());
                    if (explosions.size() > 1 && !hasMetaPermission(sender, "firework-multiple", true, true, ess)) {
                        throw new TranslatableException("multipleCharges");
                    }
                    stack.set(DataComponents.FIREWORKS, new Fireworks(fireworks.flightDuration(), explosions));
                    builder = new FireworkBuilder();
                }
                final IntList primaryColors = new IntArrayList();
                for (final String color : split[1].split(",")) {
                    if (colorMap.containsKey(color.toUpperCase())) {
                        validFirework = true;
                        primaryColors.add(colorMap.get(color.toUpperCase()).intValue());
                    } else if (hexPattern.matcher(color).matches()) {
                        validFirework = true;
                        primaryColors.add(Integer.decode(color).intValue());
                    } else {
                        throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                    }
                }
                builder.colors = primaryColors;
            } else if (split[0].equalsIgnoreCase("shape") || split[0].equalsIgnoreCase("type") || (allowShortName && (split[0].equalsIgnoreCase("s") || split[0].equalsIgnoreCase("t")))) {
                final String shape = split[1].equalsIgnoreCase("large") ? "BALL_LARGE" : split[1];
                if (fireworkShape.containsKey(shape.toUpperCase())) {
                    builder.shape = fireworkShape.get(shape.toUpperCase());
                } else {
                    throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                }
            } else if (split[0].equalsIgnoreCase("fade") || (allowShortName && split[0].equalsIgnoreCase("f"))) {
                final IntList fadeColors = new IntArrayList();
                for (final String color : split[1].split(",")) {
                    if (colorMap.containsKey(color.toUpperCase())) {
                        fadeColors.add(colorMap.get(color.toUpperCase()).intValue());
                    } else if (hexPattern.matcher(color).matches()) {
                        fadeColors.add(Integer.decode(color).intValue());
                    } else {
                        throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                    }
                }
                if (!fadeColors.isEmpty()) {
                    builder.fade = fadeColors;
                }
            } else if (split[0].equalsIgnoreCase("effect") || (allowShortName && split[0].equalsIgnoreCase("e"))) {
                for (final String effect : split[1].split(",")) {
                    if (effect.equalsIgnoreCase("twinkle")) {
                        builder.flicker = true;
                    } else if (effect.equalsIgnoreCase("trail")) {
                        builder.trail = true;
                    } else {
                        throw new TranslatableException("invalidFireworkFormat", split[1], split[0]);
                    }
                }
            }
        }
    }

    public void addPotionMeta(final CommandSource sender, final boolean allowShortName, final String string, final Essentials ess) throws Exception {
        if (MaterialUtil.isPotion(stack.getItem())) {
            final String[] split = splitPattern.split(string, 2);
            if (split.length < 2) {
                return;
            }
            if (split[0].equalsIgnoreCase("effect") || (allowShortName && split[0].equalsIgnoreCase("e"))) {
                pEffectType = Potions.getByName(split[1]);
                if (pEffectType != null) {
                    final String effectName = Potions.getName(pEffectType).toLowerCase(Locale.ENGLISH);
                    if (hasMetaPermission(sender, "potions." + effectName, true, false, ess)) {
                        validPotionEffect = true;
                    } else {
                        throw new TranslatableException("noPotionEffectPerm", effectName);
                    }
                } else {
                    throw new TranslatableException("invalidPotionMeta", split[1]);
                }
            } else if (split[0].equalsIgnoreCase("power") || (allowShortName && split[0].equalsIgnoreCase("p"))) {
                if (NumberUtil.isInt(split[1])) {
                    validPotionPower = true;
                    power = Integer.parseInt(split[1]);
                    if (power > 0 && power < 4) {
                        power -= 1;
                    }
                } else {
                    throw new TranslatableException("invalidPotionMeta", split[1]);
                }
            } else if (split[0].equalsIgnoreCase("amplifier") || (allowShortName && split[0].equalsIgnoreCase("a"))) {
                if (NumberUtil.isInt(split[1])) {
                    validPotionPower = true;
                    power = Integer.parseInt(split[1]);
                } else {
                    throw new TranslatableException("invalidPotionMeta", split[1]);
                }
            } else if (split[0].equalsIgnoreCase("duration") || (allowShortName && split[0].equalsIgnoreCase("d"))) {
                if (NumberUtil.isInt(split[1])) {
                    validPotionDuration = true;
                    final int parsed = Integer.parseInt(split[1]);
                    duration = parsed == -1 ? -1 : parsed * 20;
                } else {
                    throw new TranslatableException("invalidPotionMeta", split[1]);
                }
            } else if (split[0].equalsIgnoreCase("splash") || (allowShortName && split[0].equalsIgnoreCase("s"))) {
                isSplashPotion = Boolean.parseBoolean(split[1]);
            }
            if (isValidPotion()) {
                pEffect = new MobEffectInstance(pEffectType, duration, power);
                final PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                final List<MobEffectInstance> effects = new ArrayList<>(contents.customEffects());
                effects.removeIf(e -> e.getEffect().equals(pEffectType));
                effects.add(pEffect);
                if (effects.size() > 1 && !hasMetaPermission(sender, "potions.multiple", true, false, ess)) {
                    throw new TranslatableException("multiplePotionEffects");
                }
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(contents.potion(), contents.customColor(), effects));
                if (isSplashPotion && stack.getItem() == Items.POTION) {
                    final ItemStack splash = stack.transmuteCopy(Items.SPLASH_POTION);
                    stack = splash;
                } else if (!isSplashPotion && stack.getItem() == Items.SPLASH_POTION) {
                    stack = stack.transmuteCopy(Items.POTION);
                }
                resetPotionMeta();
            }
        }
    }

    private boolean parseEnchantmentStrings(final CommandSource sender, final boolean allowUnsafe, final String[] split, final Essentials ess) throws Exception {
        final Holder<Enchantment> enchantment = Enchantments.getByName(split[0]);
        if (enchantment == null) {
            return false;
        }
        if (hasMetaPermission(sender, "enchantments." + Enchantments.getRealName(enchantment), false, false, ess)) {
            int level = -1;
            if (split.length > 1) {
                try {
                    level = Integer.parseInt(split[1]);
                } catch (final NumberFormatException ex) {
                    level = -1;
                }
            }
            if (level < 0 || (!allowUnsafe && level > enchantment.value().getMaxLevel())) {
                level = enchantment.value().getMaxLevel();
            }
            addEnchantment(sender, allowUnsafe, enchantment, level);
        }
        return true;
    }

    public void addEnchantment(final CommandSource sender, final boolean allowUnsafe, final Holder<Enchantment> enchantment, final int level) throws Exception {
        if (enchantment == null) {
            throw new TranslatableException("enchantmentNotFound");
        }
        try {
            if (!allowUnsafe && level != 0) {
                if (stack.getItem() != Items.ENCHANTED_BOOK && !enchantment.value().canEnchant(stack)) {
                    throw new IllegalArgumentException("Specified enchantment cannot be applied to this itemstack");
                }
                if (level > enchantment.value().getMaxLevel()) {
                    throw new IllegalArgumentException("Specified enchantment level is greater than max level");
                }
                final ItemEnchantments existing = stack.getItem() == Items.ENCHANTED_BOOK ? stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY) : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (final Holder<Enchantment> other : existing.keySet()) {
                    if (!other.equals(enchantment) && !Enchantment.areCompatible(other, enchantment)) {
                        throw new IllegalArgumentException("Specified enchantment conflicts with " + Enchantments.getRealName(other));
                    }
                }
            }
            final var type = stack.getItem() == Items.ENCHANTED_BOOK ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
            final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(stack.getOrDefault(type, ItemEnchantments.EMPTY));
            if (level == 0) {
                mutable.removeIf(h -> h.equals(enchantment));
            } else {
                mutable.set(enchantment, level);
            }
            stack.set(type, mutable.toImmutable());
        } catch (final Exception ex) {
            throw new Exception("Enchantment " + Enchantments.getRealName(enchantment) + ": " + ex.getMessage(), ex);
        }
    }

    public Holder<Enchantment> getEnchantment(final User user, final String name) throws Exception {
        final Holder<Enchantment> enchantment = Enchantments.getByName(name);
        if (enchantment == null) {
            return null;
        }
        final String enchantmentName = Enchantments.getRealName(enchantment);
        if (!hasMetaPermission(user, "enchantments." + enchantmentName, true, false)) {
            throw new TranslatableException("enchantmentPerm", enchantmentName);
        }
        return enchantment;
    }

    public void addBannerMeta(final CommandSource sender, final boolean allowShortName, final String string, final Essentials ess) throws Exception {
        if (MaterialUtil.isBanner(stack.getItem()) && string != null) {
            final String[] split = splitPattern.split(string, 2);
            if (split.length < 2) {
                throw new TranslatableException("invalidBanner", string);
            }
            if (split[0].equalsIgnoreCase("basecolor")) {
                final DyeColor color = closestDye(Integer.parseInt(split[1]));
                if (stack.getItem() == Items.SHIELD) {
                    stack.set(DataComponents.BASE_COLOR, color);
                } else {
                    // Banner base colour is the item type itself
                    final ResourceLocation loc = ResourceLocation.withDefaultNamespace(color.getName() + "_banner");
                    if (BuiltInRegistries.ITEM.containsKey(loc)) {
                        stack = stack.transmuteCopy(BuiltInRegistries.ITEM.get(loc));
                    }
                }
            } else {
                final ResourceLocation patternKey = ResourceLocation.withDefaultNamespace(patternIdToKey(split[0]));
                final Optional<Holder.Reference<BannerPattern>> pattern = ess.getServer().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).get(ResourceKey.create(Registries.BANNER_PATTERN, patternKey));
                if (pattern.isPresent()) {
                    final DyeColor color = closestDye(Integer.parseInt(split[1]));
                    final BannerPatternLayers layers = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
                    final List<BannerPatternLayers.Layer> list = new ArrayList<>(layers.layers());
                    list.add(new BannerPatternLayers.Layer(pattern.get(), color));
                    stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(list));
                }
            }
        }
    }

    private static final Map<String, String> LEGACY_PATTERN_IDS = Map.ofEntries(
        Map.entry("b", "base"), Map.entry("bl", "square_bottom_left"), Map.entry("br", "square_bottom_right"),
        Map.entry("tl", "square_top_left"), Map.entry("tr", "square_top_right"), Map.entry("bs", "stripe_bottom"),
        Map.entry("ts", "stripe_top"), Map.entry("ls", "stripe_left"), Map.entry("rs", "stripe_right"),
        Map.entry("cs", "stripe_center"), Map.entry("ms", "stripe_middle"), Map.entry("drs", "stripe_downright"),
        Map.entry("dls", "stripe_downleft"), Map.entry("ss", "small_stripes"), Map.entry("cr", "cross"),
        Map.entry("sc", "straight_cross"), Map.entry("bt", "triangle_bottom"), Map.entry("tt", "triangle_top"),
        Map.entry("bts", "triangles_bottom"), Map.entry("tts", "triangles_top"), Map.entry("ld", "diagonal_left"),
        Map.entry("rd", "diagonal_up_right"), Map.entry("lud", "diagonal_up_left"), Map.entry("rud", "diagonal_right"),
        Map.entry("mc", "circle"), Map.entry("mr", "rhombus"), Map.entry("vh", "half_vertical"),
        Map.entry("hh", "half_horizontal"), Map.entry("vhr", "half_vertical_right"), Map.entry("hhb", "half_horizontal_bottom"),
        Map.entry("bo", "border"), Map.entry("cbo", "curly_border"), Map.entry("cre", "creeper"),
        Map.entry("gra", "gradient"), Map.entry("gru", "gradient_up"), Map.entry("bri", "bricks"),
        Map.entry("sku", "skull"), Map.entry("flo", "flower"), Map.entry("moj", "mojang"),
        Map.entry("glb", "globe"), Map.entry("pig", "piglin"), Map.entry("flw", "flow"), Map.entry("gus", "guster")
    );

    private static String patternIdToKey(final String id) {
        final String lower = id.toLowerCase(Locale.ENGLISH);
        return LEGACY_PATTERN_IDS.getOrDefault(lower, lower);
    }

    private static DyeColor closestDye(final int rgb) {
        DyeColor best = DyeColor.WHITE;
        double bestDist = Double.MAX_VALUE;
        for (final DyeColor color : DyeColor.values()) {
            final int c = color.getTextureDiffuseColor();
            final int r1 = (c >> 16) & 0xFF, g1 = (c >> 8) & 0xFF, b1 = c & 0xFF;
            final int r2 = (rgb >> 16) & 0xFF, g2 = (rgb >> 8) & 0xFF, b2 = rgb & 0xFF;
            final double dist = Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
            if (dist < bestDist) {
                bestDist = dist;
                best = color;
            }
        }
        return best;
    }

    private boolean hasMetaPermission(final CommandSource sender, final String metaPerm, final boolean graceful, final boolean includeBase, final Essentials ess) throws Exception {
        final User user = sender != null && sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        return hasMetaPermission(user, metaPerm, graceful, includeBase);
    }

    private boolean hasMetaPermission(final User user, final String metaPerm, final boolean graceful, final boolean includeBase) throws Exception {
        final String permBase = includeBase ? "essentials.itemspawn.meta-" : "essentials.";
        if (user == null || user.isAuthorized(permBase + metaPerm)) {
            return true;
        }
        if (graceful) {
            return false;
        } else {
            throw new TranslatableException("noMetaPerm", metaPerm);
        }
    }

    public static void setUnbreakable(final ItemStack is, final boolean unbreakable) {
        if (unbreakable) {
            is.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        } else {
            is.remove(DataComponents.UNBREAKABLE);
        }
    }

    /**
     * Mutable firework explosion builder mirroring Bukkit's FireworkEffect.Builder.
     */
    public static final class FireworkBuilder {
        public FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
        public IntList colors = new IntArrayList();
        public IntList fade = new IntArrayList();
        public boolean trail;
        public boolean flicker;

        public FireworkExplosion build() {
            return new FireworkExplosion(shape, colors, fade, trail, flicker);
        }
    }
}
