package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.ChatColor;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandsell extends EssentialsCommand {
    public Commandsell() {
        super("sell");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        BigDecimal totalWorth = BigDecimal.ZERO;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        if (args[0].equalsIgnoreCase("hand") && !user.isAuthorized("essentials.sell.hand")) {
            throw new TranslatableException("sellHandPermission");
        } else if ((args[0].equalsIgnoreCase("inventory") || args[0].equalsIgnoreCase("invent") || args[0].equalsIgnoreCase("all")) && !user.isAuthorized("essentials.sell.bulk")) {
            throw new TranslatableException("sellBulkPermission");
        }
        final List<ItemStack> is = ess.getItemDb().getMatching(user, args);
        int count = 0;
        final boolean isBulk = is.size() > 1;
        final List<ItemStack> notSold = new ArrayList<>();
        for (ItemStack stack : is) {
            if (!ess.getSettings().isAllowSellNamedItems()) {
                if (MaterialUtil.hasCustomName(stack)) {
                    if (isBulk) {
                        notSold.add(stack);
                        continue;
                    }
                    throw new TranslatableException("cannotSellNamedItem");
                }
            }
            try {
                if (stack.getCount() > 0) {
                    totalWorth = totalWorth.add(sellItem(user, stack, args, isBulk));
                    stack = stack.copy();
                    count++;
                    for (final ItemStack zeroStack : is) {
                        if (ItemStack.isSameItemSameComponents(zeroStack, stack)) {
                            zeroStack.setCount(0);
                        }
                    }
                }
            } catch (final Exception e) {
                if (!isBulk) {
                    throw e;
                }
            }
        }
        if (!notSold.isEmpty()) {
            final List<String> names = new ArrayList<>();
            for (final ItemStack stack : notSold) {
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    names.add(Text.get().nativeToLegacy(stack.get(DataComponents.CUSTOM_NAME)));
                }
            }
            ess.showError(user.getSource(), new TranslatableException("cannotSellTheseNamedItems", String.join(ChatColor.RESET + ", ", names)), commandLabel);
        }
        if (count != 1) {
            final Text.ParsedPlaceholder totalWorthStr = Text.parsed(NumberUtil.displayCurrency(totalWorth, ess));
            if (args[0].equalsIgnoreCase("blocks")) {
                user.sendTl("totalWorthBlocks", totalWorthStr, totalWorthStr);
            } else {
                user.sendTl("totalWorthAll", totalWorthStr, totalWorthStr);
            }
        }
    }

    private BigDecimal sellItem(final User user, final ItemStack is, final String[] args, final boolean isBulkSell) throws Exception {
        final int amount = ess.getWorth().getAmount(ess, user, is, args, isBulkSell);
        final BigDecimal originalWorth = ess.getWorth().getPrice(ess, is);
        final BigDecimal worth = originalWorth == null ? null : originalWorth.multiply(ess.getSettings().getMultiplier(user));
        if (worth == null) {
            throw new TranslatableException("itemCannotBeSold");
        }
        final String typeName = UserData.itemKey(is).toLowerCase(Locale.ENGLISH);
        if (amount <= 0) {
            if (!isBulkSell) {
                user.sendTl("itemSold", Text.parsed(NumberUtil.displayCurrency(BigDecimal.ZERO, ess)), BigDecimal.ZERO, typeName, NumberUtil.displayCurrency(worth, ess));
            }
            return BigDecimal.ZERO;
        }
        final BigDecimal result = worth.multiply(BigDecimal.valueOf(amount));
        final ItemStack ris = is.copy();
        ris.setCount(amount);
        if (!Inventories.containsAtLeast(user.getBase(), ris, amount)) {
            // This should never happen.
            throw new IllegalStateException("Trying to remove more items than are available.");
        }
        Inventories.removeItemAmount(user.getBase(), ris, ris.getCount());
        Inventories.update(user.getBase());
        Trade.log("Command", "Sell", "Item", user.getName(), new Trade(ris, ess), user.getName(), new Trade(result, ess), user.getLocation(), user.getMoney(), ess);
        user.giveMoney(result, null);
        final Text.ParsedPlaceholder worthDisplay = Text.parsed(NumberUtil.displayCurrency(worth, ess));
        user.sendTl("itemSold", Text.parsed(NumberUtil.displayCurrency(result, ess)), amount, typeName, worthDisplay);
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("itemSoldConsole", user.getName(), typeName, Text.get().miniToLegacy(NumberUtil.displayCurrency(result, ess)), amount, Text.get().miniToLegacy(worthDisplay.toString()), user.getDisplayName()))));
        return result;
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getMatchingItems(args[0]);
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("1", "64"));
        } else {
            return Collections.emptyList();
        }
    }
}
