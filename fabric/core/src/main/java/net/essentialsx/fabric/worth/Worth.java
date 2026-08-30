package net.essentialsx.fabric.worth;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.config.YamlFile;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Item sell values from {@code worth.yml}.
 */
public class Worth {
    private final YamlFile config;

    public Worth(final Path dataFolder) {
        config = new YamlFile(dataFolder.resolve("worth.yml"), "/worth.yml");
        config.load();
    }

    private static String key(final ItemStack itemStack) {
        return UserData.itemKey(itemStack).toLowerCase(Locale.ENGLISH).replace("_", "").replace(":", "");
    }

    public BigDecimal getPrice(final Essentials ess, final ItemStack itemStack) {
        BigDecimal result = BigDecimal.ONE.negate();
        final String itemname = key(itemStack);
        final Map<String, Object> itemNameMatch = config.getSection("worth." + itemname);
        if (itemNameMatch != null && itemNameMatch.size() == 1) {
            result = config.getBigDecimal("worth." + itemname + ".0", BigDecimal.ONE.negate());
        }
        if (result.signum() < 0) {
            result = config.getBigDecimal("worth." + itemname + ".*", BigDecimal.ONE.negate());
        }
        if (result.signum() < 0) {
            result = config.getBigDecimal("worth." + itemname, BigDecimal.ONE.negate());
        }
        if (result.signum() < 0) {
            return null;
        }
        return result;
    }

    public int getAmount(final Essentials ess, final User user, final ItemStack is, final String[] args, final boolean isBulkSell) throws Exception {
        if (is == null || is.isEmpty()) {
            throw new TranslatableException("itemSellAir");
        }
        int amount = 0;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
            } catch (final NumberFormatException ex) {
                throw new NotEnoughArgumentsException(ex);
            }
            if (args[1].startsWith("-")) {
                amount = -amount;
            }
        }
        final boolean stack = args.length > 1 && args[1].endsWith("s");
        final boolean requireStack = ess.getSettings().isTradeInStacks(is.getItem());
        if (requireStack && !stack) {
            throw new TranslatableException("itemMustBeStacked");
        }
        int max = 0;
        for (final ItemStack s : Inventories.getInventory(user.getBase(), false)) {
            if (s == null || s.isEmpty() || !ItemStack.isSameItemSameComponents(s, is)) {
                continue;
            }
            max += s.getCount();
        }
        if (stack) {
            amount *= is.getMaxStackSize();
        }
        if (amount < 1) {
            amount += max;
        }
        if (requireStack) {
            amount -= amount % is.getMaxStackSize();
        }
        if (amount > max || amount < 1) {
            if (!isBulkSell) {
                user.sendTl("itemNotEnough2");
                user.sendTl("itemNotEnough3");
                throw new TranslatableException("itemNotEnough1");
            } else {
                return amount;
            }
        }
        return amount;
    }

    public void setPrice(final Essentials ess, final ItemStack itemStack, final double price) {
        final String path = "worth." + key(itemStack);
        config.setProperty(path, price);
        config.save();
    }

    public Path getFile() {
        return config.getFile();
    }

    public void reloadConfig() {
        config.load();
    }
}
