package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Enchantments;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Locale;

public class SignEnchant extends EssentialsSign {
    public SignEnchant() {
        super("Enchant");
    }

    private static boolean tagExists(final String name) {
        final ResourceLocation loc = ResourceLocation.tryParse(name.startsWith("#") ? name.substring(1) : name);
        if (loc == null) {
            return false;
        }
        return BuiltInRegistries.ITEM.getTag(TagKey.create(net.minecraft.core.registries.Registries.ITEM, loc)).isPresent();
    }

    private static boolean isTagged(final String name, final Item item) {
        final ResourceLocation loc = ResourceLocation.tryParse(name.startsWith("#") ? name.substring(1) : name);
        if (loc == null) {
            return false;
        }
        return new ItemStack(item).is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, loc));
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final ItemStack stack;
        final String itemName = sign.getLine(1);
        try {
            stack = itemName.equals("*") || itemName.equalsIgnoreCase("any") || tagExists(itemName) ? null : getItemStack(sign.getLine(1), 1, ess);
        } catch (final SignException e) {
            sign.setLine(1, "§c<item|any>");
            throw e;
        }
        final String[] enchantLevel = sign.getLine(2).split(":");
        int level = 1;
        final Holder<Enchantment> enchantment = Enchantments.getByName(enchantLevel[0]);
        if (enchantment == null) {
            sign.setLine(2, "§c<enchant>");
            throw new SignException("enchantmentNotFound");
        }
        if (enchantLevel.length > 1) {
            try {
                level = Integer.parseInt(enchantLevel[1]);
            } catch (final NumberFormatException ex) {
                sign.setLine(2, "§c<enchant>");
                throw new SignException(ex, "errorWithMessage", ex.getMessage());
            }
        }
        final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && player.isAuthorized("essentials.enchantments.allowunsafe") && player.isAuthorized("essentials.signs.enchant.allowunsafe");
        if (level < 0 || (!allowUnsafe && level > enchantment.value().getMaxLevel())) {
            level = enchantment.value().getMaxLevel();
            sign.setLine(2, enchantLevel[0] + ":" + level);
        }
        try {
            if (stack != null) {
                final MetaItemStack meta = new MetaItemStack(stack);
                meta.addEnchantment(null, allowUnsafe, enchantment, level);
            }
        } catch (final Throwable ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        getTrade(sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final ItemStack playerHand = Inventories.getItemInHand(player.getBase());
        final String itemName = sign.getLine(1);
        final ItemStack search = itemName.equals("*") || itemName.equalsIgnoreCase("any") || (tagExists(itemName) && isTagged(itemName, playerHand.getItem())) ? null : getItemStack(itemName, 1, ess);
        final Trade charge = getTrade(sign, 3, ess);
        charge.isAffordableFor(player);
        final String[] enchantLevel = sign.getLine(2).split(":");
        final Holder<Enchantment> enchantment = Enchantments.getByName(enchantLevel[0]);
        if (enchantment == null) {
            throw new SignException("enchantmentNotFound");
        }
        int level = 1;
        if (enchantLevel.length > 1) {
            try {
                level = Integer.parseInt(enchantLevel[1]);
            } catch (final NumberFormatException ex) {
                throw new SignException(ex, "errorWithMessage", ex.getMessage());
            }
        }
        if (playerHand == null || playerHand.isEmpty() || playerHand.getCount() != 1 || (EnchantmentHelper.getItemEnchantmentLevel(enchantment, playerHand) == level)) {
            throw new SignException("missingItems", 1, sign.getLine(1));
        }
        if (search != null && playerHand.getItem() != search.getItem()) {
            throw new SignException("missingItems", 1, UserData.itemKey(search).toLowerCase(Locale.ENGLISH).replace('_', ' '));
        }
        try {
            final MetaItemStack meta = new MetaItemStack(playerHand);
            final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && player.isAuthorized("essentials.signs.enchant.allowunsafe");
            meta.addEnchantment(null, allowUnsafe, enchantment, level);
            Inventories.setItemInHand(player.getBase(), meta.getItemStack());
        } catch (final Exception ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        final String enchantmentName = Enchantments.getRealName(enchantment);
        if (level == 0) {
            player.sendTl("enchantmentRemoved", enchantmentName.replace('_', ' '));
        } else {
            player.sendTl("enchantmentApplied", enchantmentName.replace('_', ' '));
        }
        charge.charge(player);
        Trade.log("Sign", "Enchant", "Interact", username, charge, username, charge, sign.getLocation(), player.getMoney(), ess);
        Inventories.update(player.getBase());
        return true;
    }
}
