package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.Enchantments;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Commandenchant extends EssentialsCommand {
    public Commandenchant() {
        super("enchant");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack stack = user.getItemInHand();
        if (stack == null || stack.isEmpty()) {
            throw new TranslatableException("nothingInHand");
        }
        if (args.length == 0) {
            final Set<String> usableEnchants = new TreeSet<>();
            for (final Map.Entry<String, String> entry : Enchantments.entrySet()) {
                final Holder<Enchantment> holder = Enchantments.getByName(entry.getKey());
                if (holder == null) {
                    continue;
                }
                final String name = Enchantments.getRealName(holder);
                if (usableEnchants.contains(name) || (user.isAuthorized("essentials.enchantments." + name) && holder.value().canEnchant(stack))) {
                    usableEnchants.add(entry.getKey());
                }
            }
            throw new NotEnoughArgumentsException(user.playerTl("enchantments", StringUtil.joinList(usableEnchants.toArray())));
        }
        int level = 1;
        if (args.length > 1) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (final NumberFormatException ex) {
                throw new NotEnoughArgumentsException();
            }
        }
        final MetaItemStack metaStack = new MetaItemStack(stack);
        final Holder<Enchantment> enchantment = metaStack.getEnchantment(user, args[0]);
        metaStack.addEnchantment(user.getSource(), ess.getSettings().allowUnsafeEnchantments() && user.isAuthorized("essentials.enchantments.allowunsafe"), enchantment, level);
        Inventories.setItemInHand(user.getBase(), metaStack.getItemStack());
        Inventories.update(user.getBase());
        final String enchantName = Enchantments.getRealName(enchantment).replace('_', ' ');
        if (level == 0) {
            user.sendTl("enchantmentRemoved", enchantName);
        } else {
            user.sendTl("enchantmentApplied", enchantName);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> keys = new ArrayList<>();
            for (final Map.Entry<String, String> entry : Enchantments.entrySet()) {
                keys.add(entry.getKey());
            }
            return keys;
        } else if (args.length == 2) {
            final Holder<Enchantment> enchantment = Enchantments.getByName(args[0]);
            if (enchantment == null) {
                return Collections.emptyList();
            }
            final int min = enchantment.value().getMinLevel();
            final int max = enchantment.value().getMaxLevel();
            final List<String> options = new ArrayList<>();
            for (int i = min; i <= max; i++) {
                options.add(Integer.toString(i));
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
