package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.items.SpawnMob;

import java.util.List;

public class SignSpawnmob extends EssentialsSign {
    public SignSpawnmob() {
        super("Spawnmob");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        validateInteger(sign, 1);
        validateTrade(sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final Trade charge = getTrade(sign, 3, ess);
        charge.isAffordableFor(player);
        try {
            final List<String> mobParts = SpawnMob.mobParts(sign.getLine(2));
            final List<String> mobData = SpawnMob.mobData(sign.getLine(2));
            SpawnMob.spawnmob(ess, player.getSource(), player, mobParts, mobData, Integer.parseInt(sign.getLine(1)));
        } catch (final Exception ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        charge.charge(player);
        Trade.log("Sign", "Spawnmob", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
