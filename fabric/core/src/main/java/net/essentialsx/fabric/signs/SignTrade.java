package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.Inventories;
import net.essentialsx.fabric.items.MaterialUtil;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Player-to-player trade sign with stored stock (Section 14).
 */
public class SignTrade extends EssentialsSign {
    private static final int MAX_STOCK_LINE_LENGTH = 15;

    public SignTrade() {
        super("Trade");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        validateTrade(sign, 1, false, ess);
        validateTrade(sign, 2, true, ess);
        final Trade trade = getTrade(sign, 2, AmountType.ROUNDED, true, true, ess);
        final Trade charge = getTrade(sign, 1, AmountType.ROUNDED, false, true, ess);
        if (trade.getType() == charge.getType() && (trade.getType() != Trade.TradeType.ITEM || ItemStack.isSameItemSameComponents(trade.getItemStack(), charge.getItemStack()))) {
            throw new SignException("tradeSignSameType");
        }
        trade.isAffordableFor(player);
        setOwner(ess, player, sign, 3, "§8");
        trade.charge(player);
        Trade.log("Sign", "Trade", "Create", username, trade, username, null, sign.getLocation(), player.getMoney(), ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException, MaxMoneyException {
        if (isOwner(ess, player, sign, 3, "§8")) {
            final Trade store = rechargeSign(sign, ess, player);
            final Trade stored;
            try {
                stored = getTrade(sign, 1, AmountType.TOTAL, true, true, ess);
                subtractAmount(sign, 1, stored, ess, false);
                final Map<Integer, ItemStack> withdraw;
                try {
                    withdraw = stored.pay(player, Trade.OverflowType.RETURN);
                } catch (final Exception e) {
                    addAmount(sign, 1, stored, ess, false);
                    throw new SignException(e, "errorWithMessage", e.getMessage());
                }
                if (withdraw == null) {
                    Trade.log("Sign", "Trade", "Withdraw", username, store, username, null, sign.getLocation(), player.getMoney(), ess);
                } else {
                    setAmount(sign, 1, BigDecimal.valueOf(withdraw.get(0).getCount()), ess, false);
                    Trade.log("Sign", "Trade", "Withdraw", username, stored, username, new Trade(withdraw.get(0), ess), sign.getLocation(), player.getMoney(), ess);
                }
            } catch (final SignException e) {
                if (store == null) {
                    throw new SignException(e, "tradeSignEmptyOwner");
                }
            }
            Trade.log("Sign", "Trade", "Deposit", username, store, username, null, sign.getLocation(), player.getMoney(), ess);
        } else {
            final Trade charge = getTrade(sign, 1, AmountType.COST, false, true, ess);
            final Trade trade = getTrade(sign, 2, AmountType.COST, true, true, ess);
            charge.isAffordableFor(player);
            addAmount(sign, 1, charge, ess, true);
            subtractAmount(sign, 2, trade, ess, true);
            addAmount(sign, 1, charge, ess, false);
            subtractAmount(sign, 2, trade, ess, false);
            boolean paid;
            try {
                paid = trade.pay(player);
            } catch (final Exception e) {
                paid = false;
            }
            if (!paid) {
                subtractAmount(sign, 1, charge, ess, false);
                addAmount(sign, 2, trade, ess, false);
                throw new ChargeException("inventoryFull");
            }
            charge.charge(player);
            Trade.log("Sign", "Trade", "Interact", sign.getLine(3).length() > 2 ? sign.getLine(3).substring(2) : sign.getLine(3), charge, username, trade, sign.getLocation(), player.getMoney(), ess);
        }
        sign.updateSign();
        return true;
    }

    private Trade rechargeSign(final ISign sign, final Essentials ess, final User player) throws SignException, ChargeException {
        final Trade trade = getTrade(sign, 2, AmountType.COST, false, true, ess);
        ItemStack stack = Inventories.getItemInHand(player.getBase());
        if (trade.getItemStack() != null && stack != null && !stack.isEmpty() && trade.getItemStack().getItem() == stack.getItem() && MaterialUtil.getDamage(trade.getItemStack()) == MaterialUtil.getDamage(stack) && EnchantmentHelper.getEnchantmentsForCrafting(trade.getItemStack()).equals(EnchantmentHelper.getEnchantmentsForCrafting(stack))) {
            if (MaterialUtil.isPotion(trade.getItemStack().getItem()) && !ItemStack.isSameItemSameComponents(trade.getItemStack(), stack)) {
                return null;
            }
            final int amount = trade.getItemStack().getCount();
            if (Inventories.containsAtLeast(player.getBase(), trade.getItemStack(), amount)) {
                stack = stack.copy();
                stack.setCount(amount);
                final Trade store = new Trade(stack, ess);
                addAmount(sign, 2, store, ess, true);
                store.charge(player);
                addAmount(sign, 2, store, ess, false);
                return store;
            }
        }
        return null;
    }

    @Override
    protected boolean onSignBreak(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, MaxMoneyException {
        final String signOwner = sign.getLine(3);
        final String ownerName = signOwner.length() > 2 ? signOwner.substring(2) : signOwner;
        final boolean isOwner = isOwner(ess, player, sign, 3, "§8");
        final boolean canBreak = isOwner || player.isAuthorized("essentials.signs.trade.override");
        final boolean canCollect = isOwner || player.isAuthorized("essentials.signs.trade.override.collect");
        if (canBreak) {
            try {
                final Trade stored1 = getTrade(sign, 1, AmountType.TOTAL, false, true, ess);
                final Trade stored2 = getTrade(sign, 2, AmountType.TOTAL, false, true, ess);
                if (!canCollect) {
                    Trade.log("Sign", "Trade", "Destroy", ownerName, stored2, username, stored1, sign.getLocation(), player.getMoney(), ess);
                    return true;
                }
                final Map<Integer, ItemStack> withdraw1;
                final Map<Integer, ItemStack> withdraw2;
                try {
                    withdraw1 = stored1.pay(player, Trade.OverflowType.DROP);
                    withdraw2 = stored2.pay(player, Trade.OverflowType.DROP);
                } catch (final Exception e) {
                    throw new SignException(e, "errorWithMessage", e.getMessage());
                }
                if (withdraw1 == null && withdraw2 == null) {
                    Trade.log("Sign", "Trade", "Break", ownerName, stored2, username, stored1, sign.getLocation(), player.getMoney(), ess);
                    return true;
                }
                setAmount(sign, 1, BigDecimal.valueOf(withdraw1 == null ? 0L : withdraw1.get(0).getCount()), ess, false);
                Trade.log("Sign", "Trade", "Withdraw", ownerName, stored1, username, withdraw1 == null ? null : new Trade(withdraw1.get(0), ess), sign.getLocation(), player.getMoney(), ess);
                setAmount(sign, 2, BigDecimal.valueOf(withdraw2 == null ? 0L : withdraw2.get(0).getCount()), ess, false);
                Trade.log("Sign", "Trade", "Withdraw", ownerName, stored2, username, withdraw2 == null ? null : new Trade(withdraw2.get(0), ess), sign.getLocation(), player.getMoney(), ess);
                sign.updateSign();
            } catch (final SignException e) {
                if (player.isAuthorized("essentials.signs.trade.override")) {
                    return true;
                }
                throw e;
            }
            return false;
        } else {
            return false;
        }
    }

    private void validateSignLength(final String newLine) throws SignException {
        if (newLine.length() > MAX_STOCK_LINE_LENGTH) {
            throw new SignException("tradeSignFull");
        }
    }

    protected final void validateTrade(final ISign sign, final int index, final boolean amountNeeded, final Essentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");
        if (split.length == 1 && !amountNeeded) {
            final BigDecimal money = getMoney(split[0], ess);
            if (money != null) {
                final String newLine = NumberUtil.shortCurrency(money, ess) + ":0";
                validateSignLength(newLine);
                sign.setLine(index, newLine);
                return;
            }
        }
        if (split.length == 2 && amountNeeded) {
            final BigDecimal money = getMoney(split[0], ess);
            BigDecimal amount = getBigDecimalPositive(split[1], ess);
            if (money != null && amount != null) {
                amount = amount.subtract(amount.remainder(money));
                if (amount.compareTo(MINTRANSACTION) < 0 || money.compareTo(MINTRANSACTION) < 0) {
                    throw new SignException("moreThanZero");
                }
                final String newLine = NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.formatAsCurrency(amount);
                validateSignLength(newLine);
                sign.setLine(index, newLine);
                return;
            }
        }
        if (split.length == 2 && !amountNeeded) {
            final int amount = getIntegerPositive(split[0]);
            if (amount < 1) {
                throw new SignException("moreThanZero");
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], amount, ess).isEmpty()) {
                throw new SignException("moreThanZero");
            }
            final String newline = amount + " " + split[1] + ":0";
            validateSignLength(newline);
            sign.setLine(index, newline);
            return;
        }
        if (split.length == 3 && amountNeeded) {
            final int stackamount = getIntegerPositive(split[0]);
            int amount = getIntegerPositive(split[2]);
            amount -= amount % stackamount;
            if (amount < 1 || stackamount < 1) {
                throw new SignException("moreThanZero");
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], stackamount, ess).isEmpty()) {
                throw new SignException("moreThanZero");
            }
            final String newline = stackamount + " " + split[1] + ":" + amount;
            validateSignLength(newline);
            sign.setLine(index, newline);
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final Essentials ess) throws SignException {
        return getTrade(sign, index, amountType, notEmpty, false, ess);
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final boolean allowId, final Essentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");
        if (split.length == 2) {
            try {
                final BigDecimal money = getMoney(split[0], ess);
                final BigDecimal amount = notEmpty ? getBigDecimalPositive(split[1], ess) : getBigDecimal(split[1], ess);
                if (money != null && amount != null) {
                    return new Trade(amountType == AmountType.COST ? money : amount, ess);
                }
            } catch (final SignException e) {
                throw new SignException(e, "tradeSignEmpty");
            }
        }
        if (split.length == 3) {
            final int stackAmount = getIntegerPositive(split[0]);
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackAmount;
                }
                if (notEmpty && (amount < 1 || stackAmount < 1)) {
                    throw new SignException("tradeSignEmpty");
                }
                return new Trade(amountType == AmountType.COST ? stackAmount : amount, ess);
            } else {
                final ItemStack item = getItemStack(split[1], stackAmount, allowId, ess);
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackAmount;
                }
                if (notEmpty && (amount < 1 || stackAmount < 1 || item.isEmpty() || amount < stackAmount)) {
                    throw new SignException("tradeSignEmpty");
                }
                item.setCount(amountType == AmountType.COST ? stackAmount : amount);
                return new Trade(item, ess);
            }
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    protected final void subtractAmount(final ISign sign, final int index, final Trade trade, final Essentials ess, final boolean validationRun) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money.negate(), ess, validationRun);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-item.getCount()), ess, validationRun);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-exp), ess, validationRun);
        }
    }

    protected final void addAmount(final ISign sign, final int index, final Trade trade, final Essentials ess, final boolean validationRun) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money, ess, validationRun);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(item.getCount()), ess, validationRun);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(exp), ess, validationRun);
        }
    }

    private void changeAmount(final ISign sign, final int index, final BigDecimal value, final Essentials ess, final boolean validationRun) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");
        if (split.length == 2) {
            final BigDecimal amount = getBigDecimal(split[1], ess).add(value);
            setAmount(sign, index, amount, ess, validationRun);
            return;
        }
        if (split.length == 3) {
            final BigDecimal amount = getBigDecimal(split[2], ess).add(value);
            setAmount(sign, index, amount, ess, validationRun);
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    private void setAmount(final ISign sign, final int index, final BigDecimal value, final Essentials ess, final boolean validationRun) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("emptySignLine", index + 1);
        }
        final String[] split = line.split("[ :]+");
        if (split.length == 2) {
            final BigDecimal money = getMoney(split[0], ess);
            final BigDecimal amount = getBigDecimal(split[1], ess);
            if (money != null && amount != null) {
                final String newline = NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.formatAsCurrency(value);
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
                return;
            }
        }
        if (split.length == 3) {
            final int stackAmount = getIntegerPositive(split[0]);
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                final String newline = stackAmount + " " + split[1] + ":" + value.intValueExact();
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
            } else {
                getItemStack(split[1], stackAmount, ess);
                final String newline = stackAmount + " " + split[1] + ":" + value.intValueExact();
                validateSignLength(newline);
                if (!validationRun) {
                    sign.setLine(index, newline);
                }
            }
            return;
        }
        throw new SignException("invalidSignLine", index + 1);
    }

    public enum AmountType {
        TOTAL,
        ROUNDED,
        COST
    }
}
