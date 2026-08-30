package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.command.ChargeException;
import net.essentialsx.fabric.economy.Trade;

public class SignWeather extends EssentialsSign {
    public SignWeather() {
        super("Weather");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        if (sign.getLine(1).isEmpty() && sign.getLine(2).isEmpty() && sign.getLine(3).isEmpty()) {
            return true;
        }
        validateTrade(sign, 2, ess);
        final String timeString = sign.getLine(1);
        if ("Sun".equalsIgnoreCase(timeString)) {
            sign.setLine(1, "\u00a72Sun");
            return true;
        }
        if ("Storm".equalsIgnoreCase(timeString)) {
            sign.setLine(1, "\u00a72Storm");
            return true;
        }
        sign.setLine(1, "\u00a7c<sun|storm>");
        throw new SignException("onlySunStorm");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException, ChargeException {
        if (sign.getLine(1).isEmpty() && sign.getLine(2).isEmpty() && sign.getLine(3).isEmpty()) {
            if (player.getWorld().isRaining()) {
                player.sendTl("weatherSignStorm");
            } else {
                player.sendTl("weatherSignSun");
            }
            return true;
        }
        final Trade charge = getTrade(sign, 2, ess);
        charge.isAffordableFor(player);
        final String weatherString = sign.getLine(1);
        if ("\u00a72Sun".equalsIgnoreCase(weatherString)) {
            player.getWorld().setWeatherParameters(6000, 0, false, false);
            charge.charge(player);
            Trade.log("Sign", "WeatherSun", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            return true;
        }
        if ("\u00a72Storm".equalsIgnoreCase(weatherString)) {
            player.getWorld().setWeatherParameters(0, 6000, true, false);
            charge.charge(player);
            Trade.log("Sign", "WeatherStorm", "Interact", username, null, username, charge, sign.getLocation(), player.getMoney(), ess);
            return true;
        }
        throw new SignException("onlySunStorm");
    }
}
