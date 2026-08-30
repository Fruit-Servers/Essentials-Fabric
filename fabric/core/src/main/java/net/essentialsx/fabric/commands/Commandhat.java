package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.TriState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandhat extends EssentialsCommand {
    // The prefix for hat prevention commands
    public static final String PERM_PREFIX = "essentials.hat.prevent-type.";

    public Commandhat() {
        super("hat");
    }

    private static boolean hasBindingCurse(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || (!args[0].contains("rem") && !args[0].contains("off") && !args[0].equalsIgnoreCase("0"))) {
            final ItemStack hand = Inventories.getItemInMainHand(user.getBase());
            if (hand == null || hand.isEmpty()) {
                user.sendTl("hatFail");
                return;
            }
            final TriState wildcard = user.isAuthorizedExact(PERM_PREFIX + "*");
            final TriState material = user.isAuthorizedExact(PERM_PREFIX + UserData.itemKey(hand).toLowerCase(Locale.ENGLISH));
            if ((wildcard == TriState.TRUE && material != TriState.FALSE) || ((wildcard != TriState.TRUE) && material == TriState.TRUE)) {
                user.sendTl("hatFail");
                return;
            }
            if (hand.getMaxDamage() != 0) {
                user.sendTl("hatArmor");
                return;
            }
            final ItemStack head = Inventories.getHelmet(user.getBase());
            if (hasBindingCurse(head) && !user.isAuthorized("essentials.hat.ignore-binding")) {
                user.sendTl("hatCurse");
                return;
            }
            Inventories.setHelmet(user.getBase(), hand);
            Inventories.setItemInMainHand(user.getBase(), head == null ? ItemStack.EMPTY : head);
            Inventories.update(user.getBase());
            user.sendTl("hatPlaced");
            return;
        }
        final ItemStack head = Inventories.getHelmet(user.getBase());
        if (head == null || head.isEmpty()) {
            user.sendTl("hatEmpty");
        } else if (hasBindingCurse(head) && !user.isAuthorized("essentials.hat.ignore-binding")) {
            user.sendTl("hatCurse");
        } else {
            Inventories.setHelmet(user.getBase(), ItemStack.EMPTY);
            Inventories.addItem(user.getBase(), head);
            Inventories.update(user.getBase());
            user.sendTl("hatRemoved");
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("remove", "wear"));
        } else {
            return Collections.emptyList();
        }
    }
}
