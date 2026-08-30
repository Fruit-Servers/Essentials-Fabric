package net.essentialsx.fabric.items;

import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Potion effect lookup by Essentials names/aliases and registry keys.
 */
public final class Potions {
    private static final Map<String, String> POTIONS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASPOTIONS = new HashMap<>();

    static {
        put("speed", "speed", "fast", "runfast", "sprint", "swift");
        put("slowness", "slowness", "slow", "sluggish");
        put("haste", "haste", "superpick", "quickmine", "digspeed", "digfast", "sharp");
        put("mining_fatigue", "fatigue", "dull", "miningfatigue");
        put("strength", "strength", "strong", "bull", "attack");
        put("instant_health", "heal", "healthy", "instaheal", "instanthealth");
        put("instant_damage", "harm", "harming", "injure", "damage", "inflict", "instantdamage");
        put("jump_boost", "jump", "leap", "jumpboost");
        put("nausea", "nausea", "sick", "sickness", "confusion");
        put("regeneration", "regeneration", "regen");
        put("resistance", "resistance", "dmgresist", "armor");
        put("fire_resistance", "fireresist", "fireresistance", "resistfire");
        put("water_breathing", "waterbreath", "waterbreathing", "underwaterbreathing", "underwaterbreath", "air");
        put("invisibility", "invisibility", "invisible", "invis", "vanish", "disappear");
        put("blindness", "blindness", "blind");
        put("night_vision", "nightvision", "vision");
        put("hunger", "hunger", "hungry", "starve");
        put("weakness", "weakness", "weak");
        put("poison", "poison", "venom");
        put("wither", "wither", "decay");
        put("health_boost", "healthboost", "boost");
        put("absorption", "absorption", "absorb");
        put("saturation", "saturation", "food");
        put("glowing", "glowing", "glow");
        put("levitation", "levitation", "levitate");
        put("luck", "luck");
        put("unluck", "unluck", "badluck");
        put("slow_falling", "slowfalling", "slowfall", "featherfall");
        put("conduit_power", "conduitpower", "conduit");
        put("dolphins_grace", "dolphinsgrace", "dolphin", "dolphins");
        put("bad_omen", "badomen", "omen");
        put("hero_of_the_village", "heroofthevillage", "hero", "villagehero");
        put("darkness", "darkness", "dark");
        put("trial_omen", "trialomen");
        put("raid_omen", "raidomen");
        put("infested", "infested", "silverfish");
        put("oozing", "oozing", "ooze");
        put("weaving", "weaving", "weave");
        put("wind_charged", "windcharged", "windcharge", "wind");
    }

    private Potions() {
    }

    private static void put(final String key, final String primary, final String... aliases) {
        POTIONS.put(primary, key);
        for (final String alias : aliases) {
            ALIASPOTIONS.put(alias, key);
        }
    }

    public static Holder<MobEffect> getByName(final String name) {
        final String lower = name.toLowerCase(Locale.ENGLISH);
        String key = POTIONS.get(lower);
        if (key == null) {
            key = ALIASPOTIONS.get(lower);
        }
        if (key == null && NumberUtil.isInt(name)) {
            final MobEffect byId = BuiltInRegistries.MOB_EFFECT.byId(Integer.parseInt(name));
            return byId == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(byId);
        }
        if (key == null) {
            key = lower;
        }
        final ResourceLocation loc = ResourceLocation.tryParse(key);
        if (loc == null || !BuiltInRegistries.MOB_EFFECT.containsKey(loc)) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.getHolder(loc).map(h -> (Holder<MobEffect>) h).orElse(null);
    }

    public static String getName(final Holder<MobEffect> effect) {
        final ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (loc == null) {
            return "unknown";
        }
        final String path = loc.getPath();
        for (final Map.Entry<String, String> entry : POTIONS.entrySet()) {
            if (entry.getValue().equals(path)) {
                return entry.getKey();
            }
        }
        return loc.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? path : loc.toString();
    }

    public static Set<Map.Entry<String, String>> entrySet() {
        return POTIONS.entrySet();
    }
}
