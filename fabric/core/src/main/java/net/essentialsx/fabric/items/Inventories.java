package net.essentialsx.fabric.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Server-side inventory helpers with EssentialsX semantics (oversized stacks, auto-equip,
 * space reservation) on top of the vanilla {@link Inventory}.
 */
public final class Inventories {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 36;
    private static final int HELMET = 39;
    private static final int CHESTPLATE = 38;
    private static final int LEGGINGS = 37;
    private static final int BOOTS = 36;
    private static final int OFFHAND = 40;

    private Inventories() {
    }

    public static ItemStack getItemInHand(final ServerPlayer player) {
        final ItemStack main = player.getMainHandItem();
        if (!main.isEmpty()) {
            return main;
        }
        return player.getOffhandItem();
    }

    public static ItemStack getItemInMainHand(final ServerPlayer player) {
        return player.getMainHandItem();
    }

    public static void setItemInMainHand(final ServerPlayer player, final ItemStack stack) {
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
    }

    public static void setItemInHand(final ServerPlayer player, final ItemStack stack) {
        if (player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, stack);
        } else {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        }
    }

    public static ItemStack[] getInventory(final ServerPlayer player, final boolean includeArmor) {
        final Inventory inv = player.getInventory();
        final int size = includeArmor ? inv.getContainerSize() : MAIN_SIZE;
        final ItemStack[] result = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            result[i] = inv.getItem(i);
        }
        return result;
    }

    public static void setInventory(final ServerPlayer player, final ItemStack[] contents) {
        final Inventory inv = player.getInventory();
        for (int i = 0; i < contents.length && i < inv.getContainerSize(); i++) {
            inv.setItem(i, contents[i] == null ? ItemStack.EMPTY : contents[i]);
        }
        player.inventoryMenu.broadcastChanges();
    }

    public static boolean isBottomInventorySlot(final int rawSlot) {
        return rawSlot >= 0 && rawSlot < 36;
    }

    public static boolean containsAtLeast(final ServerPlayer player, final ItemStack item, final int amount) {
        int found = 0;
        final Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            final ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                found += stack.getCount();
                if (found >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int count(final ServerPlayer player, final Predicate<ItemStack> filter) {
        int found = 0;
        final Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            final ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    public static void removeItemAmount(final ServerPlayer player, final ItemStack item, int amount) {
        final Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            final ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                final int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    public static void removeItemExact(final ServerPlayer player, final ItemStack item, int amount) {
        removeItemAmount(player, item, amount);
    }

    /**
     * Remove items matching the predicate; returns the removed stacks.
     */
    public static List<ItemStack> removeItems(final ServerPlayer player, final Predicate<ItemStack> filter, final boolean includeArmor) {
        final List<ItemStack> removed = new ArrayList<>();
        final Inventory inv = player.getInventory();
        final int size = includeArmor ? inv.getContainerSize() : MAIN_SIZE;
        for (int i = 0; i < size; i++) {
            final ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                removed.add(stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        player.inventoryMenu.broadcastChanges();
        return removed;
    }

    public static void clearAll(final ServerPlayer player) {
        player.getInventory().clearContent();
        player.inventoryMenu.broadcastChanges();
    }

    private static int effectiveMax(final ItemStack stack, final int maxStackSize) {
        if (maxStackSize > 0) {
            return maxStackSize;
        }
        return stack.getMaxStackSize();
    }

    private static int armorSlotFor(final ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            final EquipmentSlot slot = armor.getEquipmentSlot();
            return switch (slot) {
                case HEAD -> HELMET;
                case CHEST -> CHESTPLATE;
                case LEGS -> LEGGINGS;
                case FEET -> BOOTS;
                default -> -1;
            };
        }
        if (stack.getItem() instanceof ElytraItem) {
            return CHESTPLATE;
        }
        if (stack.getItem() instanceof ShieldItem) {
            return OFFHAND;
        }
        return -1;
    }

    /**
     * Simulate adding items; returns whether everything fits.
     */
    public static boolean hasSpace(final ServerPlayer player, final int maxStackSize, final boolean autoEquip, final ItemStack... items) {
        final ItemStack[] sim = new ItemStack[41];
        final Inventory inv = player.getInventory();
        for (int i = 0; i < 41; i++) {
            sim[i] = inv.getItem(i).copy();
        }
        for (final ItemStack item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            final ItemStack remaining = addToArray(sim, item.copy(), maxStackSize, autoEquip);
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSpace(final ServerPlayer player, final int maxStackSize, final boolean autoEquip, final List<ItemStack> items) {
        return hasSpace(player, maxStackSize, autoEquip, items.toArray(new ItemStack[0]));
    }

    private static ItemStack addToArray(final ItemStack[] slots, final ItemStack item, final int maxStackSize, final boolean autoEquip) {
        if (autoEquip) {
            final int armorSlot = armorSlotFor(item);
            if (armorSlot != -1 && (slots[armorSlot] == null || slots[armorSlot].isEmpty())) {
                final ItemStack one = item.copyWithCount(1);
                slots[armorSlot] = one;
                item.shrink(1);
                if (item.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        final int max = effectiveMax(item, maxStackSize);
        // merge into existing stacks first
        for (int i = 0; i < MAIN_SIZE && !item.isEmpty(); i++) {
            final ItemStack slot = slots[i];
            if (slot != null && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, item) && slot.getCount() < max) {
                final int move = Math.min(max - slot.getCount(), item.getCount());
                slot.grow(move);
                item.shrink(move);
            }
        }
        for (int i = 0; i < MAIN_SIZE && !item.isEmpty(); i++) {
            if (slots[i] == null || slots[i].isEmpty()) {
                final int move = Math.min(max, item.getCount());
                slots[i] = item.copyWithCount(move);
                item.shrink(move);
            }
        }
        return item.isEmpty() ? ItemStack.EMPTY : item;
    }

    /**
     * Adds items to the player's inventory; returns leftover stacks keyed by input index.
     */
    public static Map<Integer, ItemStack> addItem(final ServerPlayer player, final int maxStackSize, final boolean autoEquip, final ItemStack... items) {
        final Map<Integer, ItemStack> leftover = new HashMap<>();
        final Inventory inv = player.getInventory();
        final ItemStack[] slots = new ItemStack[41];
        for (int i = 0; i < 41; i++) {
            slots[i] = inv.getItem(i);
        }
        for (int idx = 0; idx < items.length; idx++) {
            final ItemStack item = items[idx];
            if (item == null || item.isEmpty()) {
                continue;
            }
            final ItemStack working = item.copy();
            final ItemStack remaining = addToArray(slots, working, maxStackSize, autoEquip);
            if (!remaining.isEmpty()) {
                leftover.put(idx, remaining);
            }
        }
        for (int i = 0; i < 41; i++) {
            inv.setItem(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);
        }
        player.inventoryMenu.broadcastChanges();
        return leftover;
    }

    public static Map<Integer, ItemStack> addItem(final ServerPlayer player, final ItemStack... items) {
        return addItem(player, 0, false, items);
    }

    /**
     * Adds an item to a specific slot; returns the leftover or null when everything fit.
     */
    public static ItemStack addItem(final ServerPlayer player, final int maxStackSize, final ItemStack item, final int slot) {
        final Inventory inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) {
            return item;
        }
        final ItemStack existing = inv.getItem(slot);
        final int max = effectiveMax(item, maxStackSize);
        if (existing.isEmpty()) {
            final int move = Math.min(max, item.getCount());
            inv.setItem(slot, item.copyWithCount(move));
            final ItemStack rest = item.copy();
            rest.shrink(move);
            player.inventoryMenu.broadcastChanges();
            return rest.isEmpty() ? null : rest;
        }
        if (ItemStack.isSameItemSameComponents(existing, item) && existing.getCount() < max) {
            final int move = Math.min(max - existing.getCount(), item.getCount());
            existing.grow(move);
            final ItemStack rest = item.copy();
            rest.shrink(move);
            player.inventoryMenu.broadcastChanges();
            return rest.isEmpty() ? null : rest;
        }
        return item;
    }

    public static void dropNaturally(final ServerPlayer player, final ItemStack stack) {
        int spill = stack.getCount();
        while (spill > 0) {
            final int amount = Math.min(spill, stack.getMaxStackSize());
            final ItemStack drop = stack.copyWithCount(amount);
            final ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), drop);
            entity.setDefaultPickUpDelay();
            player.level().addFreshEntity(entity);
            spill -= amount;
        }
    }

    public static ItemStack getHelmet(final ServerPlayer player) {
        return player.getInventory().getItem(HELMET);
    }

    public static void setHelmet(final ServerPlayer player, final ItemStack stack) {
        player.getInventory().setItem(HELMET, stack == null ? ItemStack.EMPTY : stack);
        player.inventoryMenu.broadcastChanges();
    }

    public static boolean isAir(final ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.is(Items.AIR);
    }

    public static void update(final ServerPlayer player) {
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}
