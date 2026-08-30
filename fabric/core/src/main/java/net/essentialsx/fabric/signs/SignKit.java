package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.kit.Kit;

import java.util.Locale;

public class SignKit extends EssentialsSign {
    public SignKit() {
        super("Kit");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        validateTrade(sign, 3, ess);
        final String kitName = sign.getLine(1).toLowerCase(Locale.ENGLISH).trim();
        if (kitName.isEmpty()) {
            sign.setLine(1, "\u00a7dKit name!");
            return false;
        } else {
            if (ess.getKits().getKit(kitName) == null) {
                throw new SignException("kitNotFound");
            }
            final String group = sign.getLine(2);
            if ("Everyone".equalsIgnoreCase(group) || "Everybody".equalsIgnoreCase(group)) {
                sign.setLine(2, "\u00a72Everyone");
            }
            return true;
        }
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final String kitName = sign.getLine(1).toLowerCase(Locale.ENGLISH).trim();
        final String group = sign.getLine(2).trim();
        if ((!group.isEmpty() && ("\u00a72Everyone".equals(group) || player.inGroup(group))) || (group.isEmpty() && player.isAuthorized("essentials.kits." + kitName))) {
            final Trade charge = getTrade(sign, 3, ess);
            charge.isAffordableFor(player);
            try {
                final Kit kit = new Kit(kitName, ess);
                kit.checkDelay(player);
                kit.setTime(player);
                kit.expandItems(player);
                charge.charge(player);
                Trade.log("Sign", "Kit", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            } catch (final NoChargeException ex) {
                return false;
            } catch (final Exception ex) {
                throw new SignException(ex, "errorWithMessage", ex.getMessage());
            }
            return true;
        } else {
            if (group.isEmpty()) {
                throw new SignException("noKitPermission", "essentials.kits." + kitName);
            } else {
                throw new SignException("noKitGroup", group);
            }
        }
    }
}
