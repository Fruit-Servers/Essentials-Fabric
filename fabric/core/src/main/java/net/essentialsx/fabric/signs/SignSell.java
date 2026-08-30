package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

public class SignSell extends EssentialsSign {
    public SignSell() {
        super("Sell");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 1, 2, player, ess);
        validateTrade(sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException, MaxMoneyException {
        Trade charge = getTrade(sign, 1, 2, player, ess);
        Trade money = getTrade(sign, 3, ess);
        if (ess.getSettings().isAllowBulkBuySell() && player.getBase().isShiftKeyDown()) {
            final ItemStack heldItem = player.getItemInHand();
            if (ItemStack.isSameItemSameComponents(charge.getItemStack(), heldItem)) {
                final int initialItemAmount = charge.getItemStack().getCount();
                final int newItemAmount = heldItem.getCount();
                final ItemStack item = charge.getItemStack();
                item.setCount(newItemAmount);
                charge = new Trade(item, ess);
                final BigDecimal chargeAmount = money.getMoney();
                BigDecimal pricePerSingleItem = chargeAmount.divide(new BigDecimal(initialItemAmount), java.math.MathContext.DECIMAL64);
                pricePerSingleItem = pricePerSingleItem.multiply(new BigDecimal(newItemAmount));
                pricePerSingleItem = pricePerSingleItem.multiply(ess.getSettings().getMultiplier(player));
                money = new Trade(pricePerSingleItem, ess);
            }
        }
        charge.isAffordableFor(player);
        charge.charge(player);
        try {
            money.pay(player, Trade.OverflowType.DROP);
        } catch (final Exception e) {
            throw new SignException(e, "errorWithMessage", e.getMessage());
        }
        Trade.log("Sign", "Sell", "Interact", username, charge, username, money, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
