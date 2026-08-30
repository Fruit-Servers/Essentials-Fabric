package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;

public class SignHeal extends EssentialsSign {
    public SignHeal() {
        super("Heal");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 1, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        if (player.getBase().getHealth() == 0) {
            throw new SignException("healDead");
        }
        final float amount = player.getBase().getMaxHealth();
        final Trade charge = getTrade(sign, 1, ess);
        charge.isAffordableFor(player);
        player.getBase().setHealth(amount);
        player.getBase().getFoodData().setFoodLevel(20);
        player.getBase().setRemainingFireTicks(0);
        player.sendTl("youAreHealed");
        charge.charge(player);
        Trade.log("Sign", "Heal", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
