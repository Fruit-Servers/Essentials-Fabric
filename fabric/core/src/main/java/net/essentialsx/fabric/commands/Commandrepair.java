package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandrepair extends EssentialsCommand {
    public Commandrepair() {
        super("repair");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || args[0].equalsIgnoreCase("hand") || !user.isAuthorized("essentials.repair.all")) {
            repairHand(user);
        } else if (args[0].equalsIgnoreCase("all")) {
            final Trade charge = new Trade("repair-all", ess);
            charge.isAffordableFor(user);
            repairAll(user);
            charge.charge(user);
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    public void repairHand(final User user) throws Exception {
        final ItemStack item = user.getItemInHand();
        if (item == null || item.isEmpty() || MaterialUtil.isBlock(item.getItem()) || MaterialUtil.getDamage(item) == 0) {
            throw new TranslatableException("repairInvalidType");
        }
        if (!item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() && !ess.getSettings().getRepairEnchanted() && !user.isAuthorized("essentials.repair.enchanted")) {
            throw new TranslatableException("repairEnchanted");
        }
        final String itemName = UserData.itemKey(item).toLowerCase(Locale.ENGLISH);
        final Trade charge = getCharge(item.getItem());
        charge.isAffordableFor(user);
        repairItem(item);
        charge.charge(user);
        Inventories.update(user.getBase());
        user.sendTl("repair", itemName.replace('_', ' '));
    }

    public void repairAll(final User user) throws Exception {
        final List<String> repaired = new ArrayList<>();
        repairItems(Inventories.getInventory(user.getBase(), false), user, repaired);
        if (user.isAuthorized("essentials.repair.armor")) {
            final ItemStack[] armor = new ItemStack[5];
            for (int i = 0; i < 5; i++) {
                armor[i] = user.getBase().getInventory().getItem(36 + i);
            }
            repairItems(armor, user, repaired);
        }
        Inventories.update(user.getBase());
        if (repaired.isEmpty()) {
            throw new TranslatableException("repairNone");
        } else {
            user.sendTl("repair", StringUtil.joinList(repaired));
        }
    }

    private void repairItem(final ItemStack item) throws Exception {
        if (MaterialUtil.isBlock(item.getItem()) || !item.isDamageableItem()) {
            throw new TranslatableException("repairInvalidType");
        }
        if (MaterialUtil.getDamage(item) == 0) {
            throw new TranslatableException("repairAlreadyFixed");
        }
        item.setDamageValue(0);
    }

    private void repairItems(final ItemStack[] items, final IUser user, final List<String> repaired) {
        for (final ItemStack item : items) {
            if (item == null || item.isEmpty() || MaterialUtil.isBlock(item.getItem()) || MaterialUtil.getDamage(item) == 0) {
                continue;
            }
            final String itemName = UserData.itemKey(item).toLowerCase(Locale.ENGLISH);
            final Trade charge = getCharge(item.getItem());
            try {
                charge.isAffordableFor(user);
            } catch (final ChargeException ex) {
                user.sendMessage(ex.getMessage());
                continue;
            }
            if (!item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() && !ess.getSettings().getRepairEnchanted() && !user.isAuthorized("essentials.repair.enchanted")) {
                continue;
            }
            try {
                repairItem(item);
            } catch (final Exception e) {
                continue;
            }
            try {
                charge.charge(user);
            } catch (final ChargeException ex) {
                user.sendMessage(ex.getMessage());
            }
            repaired.add(itemName.replace('_', ' '));
        }
    }

    private Trade getCharge(final Item material) {
        final String itemName = UserData.itemKey(material).toLowerCase(Locale.ENGLISH);
        return new Trade("repair-" + itemName.replace('_', '-'), new Trade("repair-item", ess), ess);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(List.of("hand"));
            if (user.isAuthorized("essentials.repair.all")) {
                options.add("all");
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
