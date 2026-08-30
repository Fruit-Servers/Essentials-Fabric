package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.items.Potions;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Commandpotion extends EssentialsCommand {
    public Commandpotion() {
        super("potion");
    }

    private static String effectName(final String key) {
        final Holder<MobEffect> effect = Potions.getByName(key);
        return effect == null ? key.toLowerCase(Locale.ENGLISH) : Potions.getName(effect).toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack stack = user.getItemInHand();
        if (args.length == 0) {
            final Set<String> potionslist = new TreeSet<>();
            for (final Map.Entry<String, String> entry : Potions.entrySet()) {
                final String potionName = effectName(entry.getKey());
                if (potionslist.contains(potionName) || user.isAuthorized("essentials.potions." + potionName)) {
                    potionslist.add(entry.getKey());
                }
            }
            throw new NotEnoughArgumentsException(user.playerTl("potions", StringUtil.joinList(potionslist.toArray())));
        }
        final boolean holdingPotion = stack != null && !stack.isEmpty() && MaterialUtil.isPotion(stack.getItem());
        if (holdingPotion) {
            final PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (args[0].equalsIgnoreCase("clear")) {
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(contents.potion(), contents.customColor(), List.of()));
            } else if (args[0].equalsIgnoreCase("apply") && user.isAuthorized("essentials.potion.apply")) {
                for (final MobEffectInstance effect : contents.customEffects()) {
                    user.getBase().addEffect(new MobEffectInstance(effect));
                }
            } else if (args.length < 3) {
                throw new NotEnoughArgumentsException();
            } else {
                final MetaItemStack mStack = new MetaItemStack(stack);
                for (final String arg : args) {
                    mStack.addPotionMeta(user.getSource(), true, arg, ess);
                }
                if (mStack.completePotion()) {
                    stack.set(DataComponents.POTION_CONTENTS, mStack.getItemStack().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY));
                } else {
                    user.sendTl("invalidPotion");
                    throw new NotEnoughArgumentsException();
                }
            }
            Inventories.update(user.getBase());
        } else {
            throw new TranslatableException("holdPotion");
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>();
            options.add("clear");
            if (user.isAuthorized("essentials.potion.apply")) {
                options.add("apply");
            }
            for (final Map.Entry<String, String> entry : Potions.entrySet()) {
                final String potionName = effectName(entry.getKey());
                if (user.isAuthorized("essentials.potions." + potionName)) {
                    options.add("effect:" + entry.getKey());
                }
            }
            return options;
        } else if (args.length == 2 && args[0].startsWith("effect:")) {
            return new ArrayList<>(List.of("power:1", "power:2", "power:3", "power:4", "amplifier:0", "amplifier:1", "amplifier:2", "amplifier:3"));
        } else if (args.length == 3 && args[0].startsWith("effect:")) {
            final List<String> options = new ArrayList<>();
            for (final String duration : COMMON_DURATIONS) {
                options.add("duration:" + duration);
            }
            return options;
        } else if (args.length == 4 && args[0].startsWith("effect:")) {
            return new ArrayList<>(List.of("splash:true", "splash:false"));
        } else {
            return Collections.emptyList();
        }
    }
}
