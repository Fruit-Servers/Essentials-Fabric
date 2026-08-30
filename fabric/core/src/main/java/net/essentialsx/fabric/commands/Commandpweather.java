package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.listener.PlayerTimeWeather;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class Commandpweather extends EssentialsLoopCommand {
    private static final List<String> GET_ALIASES = Arrays.asList("get", "list", "show", "display");
    private static final Map<String, PlayerTimeWeather.Weather> WEATHER_ALIASES = new HashMap<>();

    static {
        WEATHER_ALIASES.put("sun", PlayerTimeWeather.Weather.CLEAR);
        WEATHER_ALIASES.put("clear", PlayerTimeWeather.Weather.CLEAR);
        WEATHER_ALIASES.put("storm", PlayerTimeWeather.Weather.DOWNFALL);
        WEATHER_ALIASES.put("thunder", PlayerTimeWeather.Weather.DOWNFALL);
    }

    public Commandpweather() {
        super("pweather");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || GET_ALIASES.contains(args[0].toLowerCase(Locale.ENGLISH))) {
            if (args.length > 1) {
                if (args[1].equals("*") || args[1].equals("**")) {
                    sender.sendTl("pWeatherPlayers");
                }
                loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> getUserWeather(sender, player));
                return;
            }
            if (args.length == 1 || sender.isPlayer()) {
                if (sender.isPlayer()) {
                    getUserWeather(sender, sender.getUser());
                    return;
                }
                throw new NotEnoughArgumentsException();
            }
            if (ess.getOnlinePlayers().size() > 1) {
                sender.sendTl("pWeatherPlayers");
            }
            for (final User player : ess.getOnlineUsers()) {
                getUserWeather(sender, player);
            }
            return;
        }
        if (args.length > 1 && !sender.isAuthorized("essentials.pweather.others") && !args[1].equalsIgnoreCase(sender.getSelfSelector())) {
            sender.sendTl("pWeatherOthersPermission");
            return;
        }
        final String weather = args[0].toLowerCase(Locale.ENGLISH);
        if (!WEATHER_ALIASES.containsKey(weather) && !weather.equalsIgnoreCase("reset")) {
            throw new NotEnoughArgumentsException(sender.tl("pWeatherInvalidAlias"));
        }
        final StringJoiner joiner = new StringJoiner(", ");
        loopOnlinePlayersConsumer(server, sender, false, true, args.length > 1 ? args[1] : sender.getSelfSelector(), player -> {
            setUserWeather(player, weather);
            joiner.add(player.getName());
        });
        if (weather.equalsIgnoreCase("reset")) {
            sender.sendTl("pWeatherReset", joiner.toString());
            return;
        }
        sender.sendTl("pWeatherSet", weather, joiner.toString());
    }

    private void getUserWeather(final CommandSource sender, final IUser user) {
        if (user == null) {
            return;
        }
        final PlayerTimeWeather.Weather weather = ess.getPlayerTimeWeather().getPlayerWeather(user.getUUID());
        if (weather == null) {
            sender.sendTl("pWeatherNormal", user.getName());
            return;
        }
        sender.sendTl("pWeatherCurrent", user.getName(), weather.toString().toLowerCase(Locale.ENGLISH));
    }

    private void setUserWeather(final User user, final String weatherType) {
        if (weatherType.equalsIgnoreCase("reset")) {
            ess.getPlayerTimeWeather().resetPlayerWeather(user.getBase());
            return;
        }
        ess.getPlayerTimeWeather().setPlayerWeather(user.getBase(), WEATHER_ALIASES.get(weatherType));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        final User user = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        if (args.length == 1) {
            return new ArrayList<>(List.of("get", "reset", "storm", "sun"));
        } else if (args.length == 2 && (GET_ALIASES.contains(args[0]) || user == null || user.isAuthorized("essentials.pweather.others"))) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
    }
}
