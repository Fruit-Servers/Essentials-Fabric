package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.commands.Commandrepair;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.TranslatableException;

public class SignRepair extends EssentialsSign {
    public SignRepair() {
        super("Repair");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        final String repairTarget = sign.getLine(1);
        if (repairTarget.isEmpty()) {
            sign.setLine(1, "Hand");
        } else if (!repairTarget.equalsIgnoreCase("all") && !repairTarget.equalsIgnoreCase("hand")) {
            sign.setLine(1, "\u00a7c<hand|all>");
            throw new SignException("invalidSignLine", 2);
        }
        validateTrade(sign, 2, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        final Trade charge = getTrade(sign, 2, ess);
        charge.isAffordableFor(player);
        final Commandrepair command = new Commandrepair();
        command.setEssentials(ess);
        try {
            if (sign.getLine(1).equalsIgnoreCase("hand")) {
                command.repairHand(player);
            } else if (sign.getLine(1).equalsIgnoreCase("all")) {
                command.repairAll(player);
            } else {
                throw new NotEnoughArgumentsException();
            }
        } catch (final TranslatableException ex) {
            throw new SignException(ex.getTlKey(), ex.getArgs());
        } catch (final Exception ex) {
            throw new SignException(ex, "errorWithMessage", ex.getMessage());
        }
        charge.charge(player);
        Trade.log("Sign", "Repair", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
        return true;
    }
}
