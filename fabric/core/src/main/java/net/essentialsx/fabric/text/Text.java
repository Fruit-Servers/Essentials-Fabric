package net.essentialsx.fabric.text;

import com.google.gson.JsonParser;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;

import java.util.Locale;

/**
 * Text facet: MiniMessage (with Essentials {@code <primary>}/{@code <secondary>} tags),
 * legacy section/ampersand codes and conversion into native Minecraft components.
 * Mirrors the upstream {@code PaperAdventureFacet}.
 */
public final class Text {
    private static final String LOOKUP = "0123456789abcdefklmnor";
    private static final NamedTextColor[] COLORS = new NamedTextColor[] {NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE};
    private static Text INSTANCE = new Text(null, null);

    private final LegacyComponentSerializer legacySerializer;
    private final LegacyComponentSerializer legacySerializerUrls;
    private final MiniMessage miniMessageNoTags;
    private final MiniMessage miniMessageInstance;
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();
    private final String primaryColor;
    private final String secondaryColor;
    private volatile HolderLookup.Provider registries = RegistryAccess.EMPTY;

    public Text(final String primaryColor, final String secondaryColor) {
        this.primaryColor = primaryColor != null ? primaryColor : "gold";
        this.secondaryColor = secondaryColor != null ? secondaryColor : "red";
        final LegacyComponentSerializer.Builder builder = LegacyComponentSerializer.builder()
            .flattener(ComponentFlattener.basic())
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat();
        legacySerializer = builder.build();
        legacySerializerUrls = builder.extractUrls(FormatUtil.URL_PATTERN).build();
        miniMessageNoTags = MiniMessage.builder().strict(true).build();
        miniMessageInstance = MiniMessage.builder()
            .tags(TagResolver.builder()
                .resolvers(TagResolver.standard())
                .resolver(TagResolver.resolver("primary", supplyTag(true)))
                .resolver(TagResolver.resolver("secondary", supplyTag(false)))
                .build())
            .build();
    }

    public static Text get() {
        return INSTANCE;
    }

    public static void install(final Text text) {
        text.registries = INSTANCE.registries;
        INSTANCE = text;
    }

    public void setRegistries(final HolderLookup.Provider registries) {
        this.registries = registries;
    }

    private Tag supplyTag(final boolean primary) {
        final String color = primary ? primaryColor : secondaryColor;
        TextColor textColor = null;
        if (color.startsWith("#")) {
            try {
                textColor = TextColor.color(Integer.decode(color));
            } catch (final NumberFormatException ignored) {
            }
        } else {
            textColor = NamedTextColor.NAMES.value(color.toLowerCase(Locale.ENGLISH));
        }
        return textColor != null ? Tag.styling(textColor) : Tag.styling(primary ? NamedTextColor.GOLD : NamedTextColor.RED);
    }

    public Component deserializeMiniMessage(final String message) {
        return miniMessageInstance.deserialize(message);
    }

    public String legacyToMini(final String message) {
        return legacyToMini(message, false);
    }

    public String legacyToMini(final String message, final boolean useCustomTags) {
        final Component deserializedText = legacySerializer.deserialize(message);
        if (useCustomTags) {
            return miniMessageInstance.serialize(deserializedText);
        } else {
            return miniMessageNoTags.serialize(deserializedText);
        }
    }

    public String legacyToMiniWithUrls(final String message) {
        return miniMessageInstance.serialize(legacySerializerUrls.deserialize(message));
    }

    public String escapeTags(final String input) {
        return miniMessageInstance.escapeTags(input);
    }

    public String stripTags(final String input) {
        return miniMessageInstance.stripTags(input);
    }

    public Component legacyToAdventure(final String message) {
        return legacySerializer.deserialize(message);
    }

    public Component text(final String message) {
        return Component.text(message);
    }

    public String miniToLegacy(final String message) {
        return adventureToLegacy(miniMessageInstance.deserialize(message));
    }

    public String adventureToLegacy(final Component component) {
        return legacySerializer.serialize(component);
    }

    public String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public String miniToPlain(final String message) {
        return plain(miniMessageInstance.deserialize(message));
    }

    /**
     * Converts an Adventure component into a native Minecraft component through the JSON
     * representation. This avoids depending on an Adventure platform implementation.
     */
    public net.minecraft.network.chat.Component toNative(final Component component) {
        final String json = gson.serialize(component);
        final net.minecraft.network.chat.MutableComponent parsed = net.minecraft.network.chat.Component.Serializer.fromJson(JsonParser.parseString(json), registries);
        return parsed == null ? net.minecraft.network.chat.Component.empty() : parsed;
    }

    public Component fromNative(final net.minecraft.network.chat.Component component) {
        final String json = net.minecraft.network.chat.Component.Serializer.toJson(component, registries);
        return gson.deserialize(json);
    }

    public net.minecraft.network.chat.Component mini(final String message) {
        return toNative(deserializeMiniMessage(message));
    }

    public net.minecraft.network.chat.Component legacy(final String message) {
        return toNative(legacyToAdventure(message));
    }

    public String nativeToLegacy(final net.minecraft.network.chat.Component component) {
        return adventureToLegacy(fromNative(component));
    }

    public String nativeToPlain(final net.minecraft.network.chat.Component component) {
        return component.getString();
    }

    public static NamedTextColor fromChar(final char c) {
        final int index = LOOKUP.indexOf(c);
        if (index == -1 || index > 15) {
            return null;
        }
        return COLORS[index];
    }

    public static String fromCharName(final char c) {
        final NamedTextColor namedTextColor = fromChar(c);
        if (namedTextColor == null) {
            return null;
        }
        return namedTextColor.toString();
    }

    public static ParsedPlaceholder parsed(final String literal) {
        return new ParsedPlaceholder(literal);
    }

    public static String legacyColorName(final ChatColor color) {
        return color.getAdventureName();
    }

    /**
     * A placeholder whose value is already MiniMessage and must not be escaped.
     */
    public static final class ParsedPlaceholder {
        private final String value;

        ParsedPlaceholder(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
