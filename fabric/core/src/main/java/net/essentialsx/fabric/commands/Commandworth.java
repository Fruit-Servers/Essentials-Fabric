package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.UserData;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandworth extends EssentialsCommand {
    public Commandworth() {
        super("worth");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final List<ItemStack> is = ess.getItemDb().getMatching(user, args);
        int count = 0;
        final boolean isBulk = is.size() > 1;
        BigDecimal totalWorth = BigDecimal.ZERO;
        for (ItemStack stack : is) {
            try {
                if (stack.getCount() > 0) {
                    totalWorth = totalWorth.add(itemWorth(user.getSource(), user, stack, args));
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
        if (count > 1) {
            final Text.ParsedPlaceholder totalWorthStr = Text.parsed(NumberUtil.displayCurrency(totalWorth, ess));
            if (args.length > 0 && args[0].equalsIgnoreCase("blocks")) {
                user.sendTl("totalSellableBlocks", totalWorthStr, totalWorthStr);
                return;
            }
            user.sendTl("totalSellableAll", totalWorthStr, totalWorthStr);
        }
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        itemWorth(sender, null, ess.getItemDb().get(args[0]), args);
    }

    private BigDecimal itemWorth(final CommandSource sender, final User user, final ItemStack is, final String[] args) throws Exception {
        int amount = 1;
        if (user == null) {
            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
                } catch (final NumberFormatException ex) {
                    throw new NotEnoughArgumentsException(ex);
                }
            }
        } else {
            amount = ess.getWorth().getAmount(ess, user, is, args, true);
        }
        final BigDecimal worth = ess.getWorth().getPrice(ess, is);
        if (worth == null) {
            throw new TranslatableException("itemCannotBeSold");
        }
        if (amount < 0) {
            amount = 0;
        }
        final BigDecimal result = worth.multiply(BigDecimal.valueOf(amount));
        final String typeName = UserData.itemKey(is).toLowerCase(Locale.ENGLISH).replace("_", "");
        final Text.ParsedPlaceholder resultDisplay = Text.parsed(NumberUtil.displayCurrency(result, ess));
        final Text.ParsedPlaceholder worthDisplay = Text.parsed(NumberUtil.displayCurrency(worth, ess));
        if (MaterialUtil.getDamage(is) != 0) {
            sender.sendTl("worthMeta", typeName, MaterialUtil.getDamage(is), resultDisplay, amount, worthDisplay);
        } else {
            sender.sendTl("worth", typeName, resultDisplay, amount, worthDisplay);
        }
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

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("1", "64"));
        } else {
            return Collections.emptyList();
        }
    }
}
