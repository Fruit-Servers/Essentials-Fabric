package net.essentialsx.fabric.kit;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MetaItemStack;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.textreader.IText;
import net.essentialsx.fabric.textreader.KeywordReplacer;
import net.essentialsx.fabric.textreader.SimpleTextInput;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A kit definition and the transactional grant procedure (Section 11.2).
 */
public class Kit {
    final Essentials ess;
    final String kitName;
    final Map<String, Object> kit;
    final Trade charge;

    public Kit(final String kitName, final Essentials ess) throws Exception {
        this.kitName = kitName;
        this.ess = ess;
        this.kit = ess.getKits().getKit(kitName);
        this.charge = new Trade("kit-" + kitName, new Trade("kit-kit", ess), ess);
        if (kit == null) {
            throw new TranslatableException("kitNotFound");
        }
    }

    public String getName() {
        return kitName;
    }

    public void checkPerms(final User user) throws Exception {
        if (!user.isAuthorized("essentials.kits." + kitName)) {
            throw new TranslatableException("noKitPermission", "essentials.kits." + kitName);
        }
    }

    public void checkDelay(final User user) throws Exception {
        final long nextUse = getNextUse(user);
        if (nextUse == 0L) {
            return;
        } else if (nextUse < 0L) {
            user.sendTl("kitOnce");
            throw new NoChargeException();
        } else {
            user.sendTl("kitTimed", DateUtil.formatDateDiff(nextUse));
            throw new NoChargeException();
        }
    }

    public void checkAffordable(final User user) throws Exception {
        charge.isAffordableFor(user);
    }

    public void setTime(final User user) {
        final Calendar time = new GregorianCalendar();
        user.setKitTimestamp(kitName, time.getTimeInMillis());
    }

    public void resetTime(final User user) {
        user.setKitTimestamp(kitName, 0);
    }

    public void chargeUser(final User user) throws Exception {
        charge.charge(user);
    }

    public long getNextUse(final User user) throws Exception {
        if (user.isAuthorized("essentials.kit.exemptdelay")) {
            return 0L;
        }
        final Calendar time = new GregorianCalendar();
        double delay;
        try {
            delay = kit.containsKey("delay") ? ((Number) kit.get("delay")).doubleValue() : 0.0d;
        } catch (final Exception e) {
            throw new TranslatableException("kitError2");
        }
        final long lastTime = user.getKitTimestamp(kitName);
        final Calendar delayTime = new GregorianCalendar();
        delayTime.setTimeInMillis(lastTime);
        delayTime.add(Calendar.SECOND, (int) delay);
        delayTime.add(Calendar.MILLISECOND, (int) ((delay * 1000.0) % 1000.0));
        if (lastTime == 0L || lastTime > time.getTimeInMillis()) {
            return 0L;
        } else if (delay < 0d) {
            return -1;
        } else if (delayTime.before(time)) {
            return 0L;
        } else {
            return delayTime.getTimeInMillis();
        }
    }

    public List<String> getItems() throws Exception {
        if (kit == null) {
            throw new TranslatableException("kitNotFound");
        }
        try {
            final List<String> itemList = new ArrayList<>();
            final Object kitItems = kit.get("items");
            if (kitItems instanceof List) {
                for (final Object item : (List<?>) kitItems) {
                    if (item instanceof String) {
                        itemList.add(item.toString());
                        continue;
                    }
                    throw new Exception("Invalid kit item: " + item);
                }
                return itemList;
            }
            throw new Exception("Invalid item list");
        } catch (final Exception e) {
            ess.getLogger().warn("Error parsing kit " + kitName + ": " + e.getMessage());
            throw new TranslatableException(e, "kitError2");
        }
    }

    public boolean expandItems(final User user) throws Exception {
        return expandItems(user, getItems());
    }

    public boolean expandItems(final User user, final List<String> items) throws Exception {
        try {
            final IText input = new SimpleTextInput(items);
            final IText output = new KeywordReplacer(input, user.getSource(), ess, true, true);
            boolean spew = false;
            final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
            final boolean autoEquip = ess.getSettings().isKitAutoEquip();
            final List<ItemStack> itemList = new ArrayList<>();
            final Map<Integer, ItemStack> itemsWithSlot = new HashMap<>();
            final List<String> commandQueue = new ArrayList<>();
            final List<String> moneyQueue = new ArrayList<>();
            final String currencySymbol = ess.getSettings().getCurrencySymbol().isEmpty() ? "$" : ess.getSettings().getCurrencySymbol();
            for (String kitItem : output.getLines()) {
                if (kitItem.startsWith("$") || kitItem.startsWith(currencySymbol)) {
                    moneyQueue.add(NumberUtil.sanitizeCurrencyString(kitItem, ess));
                    continue;
                }
                if (kitItem.startsWith("/")) {
                    String command = kitItem.substring(1);
                    final String name = user.getName();
                    command = command.replace("{player}", name);
                    commandQueue.add(command);
                    continue;
                }
                int itemSlot = -1;
                if (kitItem.startsWith("slot:")) {
                    final int spaceIndex = kitItem.indexOf(" ");
                    if (spaceIndex != -1) {
                        final String slotStr = kitItem.substring("slot:".length(), spaceIndex);
                        itemSlot = NumberUtil.isInt(slotStr) ? Integer.parseInt(slotStr) : -1;
                        kitItem = kitItem.substring(spaceIndex + 1);
                    }
                }
                final ItemStack stack;
                if (kitItem.startsWith("@")) {
                    stack = ess.getItemSerializer().deserialize(kitItem.substring(1));
                    if (stack == null) {
                        ess.getLogger().warn("Kit {} for {} contains an item that could not be deserialized", kitName, user.getName());
                        continue;
                    }
                } else {
                    final String[] parts = kitItem.split(" +");
                    final ItemStack parseStack = ess.getItemDb().get(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
                    if (parseStack.isEmpty()) {
                        continue;
                    }
                    final MetaItemStack metaStack = new MetaItemStack(parseStack);
                    if (parts.length > 2) {
                        metaStack.parseStringMeta(null, allowUnsafe, parts, 2, ess);
                    }
                    stack = metaStack.getItemStack();
                }
                if (itemSlot == -1 || itemsWithSlot.containsKey(itemSlot)) {
                    itemList.add(stack);
                } else {
                    itemsWithSlot.put(itemSlot, stack);
                }
            }
            final int maxStackSize = user.isAuthorized("essentials.oversizedstacks") ? ess.getSettings().getOversizedStackSize() : 0;
            final boolean isDropItemsIfFull = ess.getSettings().isDropItemsIfFull();
            final List<ItemStack> totalItems = new ArrayList<>(itemList);
            totalItems.addAll(itemsWithSlot.values());
            final ItemStack[] totalItemsArray = totalItems.toArray(new ItemStack[0]);
            if (!isDropItemsIfFull && !Inventories.hasSpace(user.getBase(), maxStackSize, autoEquip, totalItemsArray)) {
                user.sendTl("kitInvFullNoDrop");
                return false;
            }
            for (final Map.Entry<Integer, ItemStack> itemWithSlot : itemsWithSlot.entrySet()) {
                final ItemStack leftover = Inventories.addItem(user.getBase(), maxStackSize, itemWithSlot.getValue(), itemWithSlot.getKey());
                if (leftover != null) {
                    itemList.add(leftover);
                }
            }
            final ItemStack[] itemArray = itemList.toArray(new ItemStack[0]);
            final Map<Integer, ItemStack> leftover = Inventories.addItem(user.getBase(), maxStackSize, autoEquip, itemArray);
            if (!isDropItemsIfFull && !leftover.isEmpty()) {
                throw new IllegalStateException("Something has gone terribly wrong while adding items to the user's inventory. Items left over: " + leftover);
            }
            for (final ItemStack itemStack : leftover.values()) {
                Inventories.dropNaturally(user.getBase(), itemStack);
                spew = true;
            }
            for (final String valueString : moneyQueue) {
                final BigDecimal value = new BigDecimal(valueString.trim());
                final Trade t = new Trade(value, ess);
                t.pay(user, Trade.OverflowType.DROP);
            }
            for (final String cmd : commandQueue) {
                ess.dispatchConsoleCommand(cmd);
            }
            if (spew) {
                user.sendTl("kitInvFull");
            }
        } catch (final Exception e) {
            Inventories.update(user.getBase());
            ess.getLogger().warn(e.getMessage());
            throw new TranslatableException(e, "kitError2");
        }
        return true;
    }
}
