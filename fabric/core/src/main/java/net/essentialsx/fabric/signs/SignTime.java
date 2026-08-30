package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.utils.DescParseTickFormat;

public class SignTime extends EssentialsSign {
    public SignTime() {
        super("Time");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        if (sign.getLine(1).isEmpty() && sign.getLine(2).isEmpty() && sign.getLine(3).isEmpty()) {
            return true;
        }
        validateTrade(sign, 2, ess);
        final String timeString = sign.getLine(1);
        if ("Day".equalsIgnoreCase(timeString)) {
            sign.setLine(1, "\u00a72Day");
            return true;
        }
        if ("Night".equalsIgnoreCase(timeString)) {
            sign.setLine(1, "\u00a72Night");
            return true;
        }
        throw new SignException("onlyDayNight");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        if (sign.getLine(1).isEmpty() && sign.getLine(2).isEmpty() && sign.getLine(3).isEmpty()) {
            player.sendTl("timeWorldCurrentSign", DescParseTickFormat.format(player.getWorld().getDayTime()));
            return true;
        }
        final Trade charge = getTrade(sign, 2, ess);
        charge.isAffordableFor(player);
        final String timeString = sign.getLine(1);
        long time = player.getWorld().getDayTime();
        time -= time % 24000;
        if ("\u00a72Day".equalsIgnoreCase(timeString)) {
            player.getWorld().setDayTime(time + 24000);
            charge.charge(player);
            Trade.log("Sign", "TimeDay", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            return true;
        }
        if ("\u00a72Night".equalsIgnoreCase(timeString)) {
            player.getWorld().setDayTime(time + 37700);
            charge.charge(player);
            Trade.log("Sign", "TimeNight", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            return true;
        }
        throw new SignException("onlyDayNight");
    }
}
