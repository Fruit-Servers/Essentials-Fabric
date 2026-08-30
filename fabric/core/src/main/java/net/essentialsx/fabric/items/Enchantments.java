package net.essentialsx.fabric.items;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Enchantment lookup by Essentials names/aliases and registry keys. Enchantments are data
 * driven in 1.21.1, so the registry is resolved from the server registry access.
 */
public final class Enchantments {
    private static final Map<String, String> ENCHANTMENTS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASENCHANTMENTS = new HashMap<>();
    private static volatile HolderLookup.Provider registries = RegistryAccess.EMPTY;

    static {
        put("sharpness", "sharpness", "alldamage", "alldmg", "sharp", "dal");
        put("bane_of_arthropods", "baneofarthropods", "ardmg", "baneofarthropod", "arthropod", "dar");
        put("smite", "smite", "undeaddamage", "du");
        put("efficiency", "efficiency", "digspeed", "minespeed", "cutspeed", "ds", "eff");
        put("unbreaking", "unbreaking", "durability", "dura", "d");
        put("thorns", "thorns", "highcrit", "thorn", "highercrit", "t");
        put("fire_aspect", "fireaspect", "fire", "meleefire", "meleeflame", "fa");
        put("knockback", "knockback", "kback", "kb", "k");
        put("fortune", "fortune", "blockslootbonus", "fort", "lbb");
        put("looting", "looting", "mobslootbonus", "mobloot", "lbm");
        put("respiration", "respiration", "oxygen", "breathing", "breath", "o");
        put("protection", "protection", "prot", "protect", "p");
        put("blast_protection", "blastprotection", "explosionsprotection", "explosionprotection", "expprot", "bprotection", "bprotect", "blastprotect", "pe");
        put("feather_falling", "featherfalling", "fallprotection", "fallprot", "featherfall", "pfa");
        put("fire_protection", "fireprotection", "flameprotection", "fireprotect", "flameprotect", "fireprot", "flameprot", "pf");
        put("projectile_protection", "projectileprotection", "projprot", "pp");
        put("silk_touch", "silktouch", "softtouch", "st");
        put("aqua_affinity", "aquaaffinity", "waterworker", "watermine", "ww");
        put("flame", "flame", "firearrow", "flamearrow", "af");
        put("power", "power", "arrowdamage", "arrowpower", "ad");
        put("punch", "punch", "arrowknockback", "arrowkb", "arrowpunch", "ak");
        put("infinity", "infinity", "infinitearrows", "infarrows", "infinite", "unlimited", "unlimitedarrows", "ai");
        put("luck_of_the_sea", "luck", "luckofsea", "luckofseas", "rodluck", "luckofthesea");
        put("lure", "lure", "rodlure");
        put("depth_strider", "depthstrider", "depth", "strider");
        put("frost_walker", "frostwalker", "frost", "walker");
        put("mending", "mending");
        put("binding_curse", "bindingcurse", "bindcurse", "binding", "bind");
        put("vanishing_curse", "vanishingcurse", "vanishcurse", "vanishing", "vanish");
        put("sweeping_edge", "sweepingedge", "sweeping", "sweepedge", "sweep");
        put("channeling", "channeling", "channelling", "chanelling", "chaneling", "channel");
        put("impaling", "impaling", "impale", "oceandamage", "oceandmg");
        put("loyalty", "loyalty", "loyal", "return");
        put("riptide", "riptide", "rip", "tide", "launch");
        put("multishot", "multishot", "tripleshot");
        put("quick_charge", "quickcharge", "quickdraw", "fastcharge", "fastdraw");
        put("piercing", "piercing");
        put("soul_speed", "soulspeed", "soilspeed", "sandspeed");
        put("swift_sneak", "swiftsneak");
        put("wind_burst", "windburst", "wind", "burst");
        put("density", "density", "dense");
        put("breach", "breach");
    }

    private Enchantments() {
    }

    private static void put(final String key, final String primary, final String... aliases) {
        ENCHANTMENTS.put(primary, key);
        for (final String alias : aliases) {
            ALIASENCHANTMENTS.put(alias, key);
        }
    }

    public static void setRegistries(final HolderLookup.Provider provider) {
        registries = provider;
    }

    private static HolderLookup.RegistryLookup<Enchantment> lookup() {
        return registries.lookupOrThrow(Registries.ENCHANTMENT);
    }

    public static Holder<Enchantment> getByName(final String name) {
        final String lower = name.toLowerCase(Locale.ENGLISH);
        String key = ENCHANTMENTS.get(lower);
        if (key == null) {
            key = ALIASENCHANTMENTS.get(lower);
        }
        if (key == null) {
            key = lower;
        }
        final ResourceLocation loc = ResourceLocation.tryParse(key);
        if (loc == null) {
            return null;
        }
        return lookup().get(ResourceKey.create(Registries.ENCHANTMENT, loc)).map(h -> (Holder<Enchantment>) h).orElse(null);
    }

    public static String getRealName(final Holder<Enchantment> enchantment) {
        final ResourceLocation loc = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (loc == null) {
            return "unknown";
        }
        final String path = loc.getPath();
        for (final Map.Entry<String, String> entry : ENCHANTMENTS.entrySet()) {
            if (entry.getValue().equals(path)) {
                return entry.getKey();
            }
        }
        return loc.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? path : loc.toString();
    }

    public static Set<Map.Entry<String, String>> entrySet() {
        return ENCHANTMENTS.entrySet();
    }

    public static Iterable<Holder.Reference<Enchantment>> all() {
        return lookup().listElements().toList();
    }

    public static Registry<Enchantment> registry() {
        return ((RegistryAccess) registries).registryOrThrow(Registries.ENCHANTMENT);
    }
}
