package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.MaxMoneyException;
import net.essentialsx.fabric.economy.Trade;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

public class SignBuy extends EssentialsSign {
    public SignBuy() {
        super("Buy");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 1, 2, player, ess);
        validateTrade(sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException, MaxMoneyException {
        Trade items = getTrade(sign, 1, 2, player, ess);
        Trade charge = getTrade(sign, 3, ess);
        if (ess.getSettings().isAllowBulkBuySell() && player.getBase().isShiftKeyDown()) {
            final ItemStack heldItem = player.getItemInHand();
            if (ItemStack.isSameItemSameComponents(items.getItemStack(), heldItem)) {
                final int initialItemAmount = items.getItemStack().getCount();
                final int newItemAmount = heldItem.getCount();
                final ItemStack item = items.getItemStack();
                item.setCount(newItemAmount);
                items = new Trade(item, ess);
                final BigDecimal chargeAmount = charge.getMoney();
                BigDecimal pricePerSingleItem = chargeAmount.divide(new BigDecimal(initialItemAmount), java.math.MathContext.DECIMAL64);
                pricePerSingleItem = pricePerSingleItem.multiply(new BigDecimal(newItemAmount));
                charge = new Trade(pricePerSingleItem, ess);
            }
        }
        charge.isAffordableFor(player);
        try {
            if (!items.pay(player)) {
                throw new ChargeException("inventoryFull");
            }
        } catch (final ChargeException e) {
            throw e;
        } catch (final Exception e) {
            throw new SignException(e, "errorWithMessage", e.getMessage());
        }
        charge.charge(player);
        Trade.log("Sign", "Buy", "Interact", username, charge, username, items, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
