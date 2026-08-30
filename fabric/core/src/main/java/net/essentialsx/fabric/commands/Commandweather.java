package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandweather extends EssentialsCommand {
    public Commandweather() {
        super("weather");
    }

    /** Mirrors Bukkit World#setStorm/#setWeatherDuration (duration 0 = vanilla default lengths). */
    public static void setStorm(final ServerLevel world, final boolean storm, final int durationTicks) {
        final int duration = durationTicks > 0 ? durationTicks : (storm ? 6000 : 6000);
        world.setWeatherParameters(storm ? 0 : duration, storm ? duration : 0, storm, storm && world.isThundering());
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final boolean isStorm;
        if (args.length == 0) {
            if (commandLabel.endsWith("sun")) {
                isStorm = false;
            } else if (commandLabel.endsWith("storm") || commandLabel.endsWith("rain")) {
                isStorm = true;
            } else {
                throw new NotEnoughArgumentsException();
            }
        } else {
            isStorm = args[0].equalsIgnoreCase("storm");
        }
        final ServerLevel world = user.getWorld();
        if (args.length > 1) {
            setStorm(world, isStorm, Integer.parseInt(args[1]) * 20);
            user.sendTl(isStorm ? "weatherStormFor" : "weatherSunFor", Worlds.name(world), args[1]);
            return;
        }
        setStorm(world, isStorm, 0);
        user.sendTl(isStorm ? "weatherStorm" : "weatherSun", Worlds.name(world));
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("When running from console, usage is: /" + commandLabel + " <world> <storm/sun> [duration]");
        }
        final boolean isStorm = args[1].equalsIgnoreCase("storm");
        final ServerLevel world = Worlds.get(server, args[0]);
        if (world == null) {
            throw new TranslatableException("weatherInvalidWorld", args[0]);
        }
        if (args.length > 2) {
            setStorm(world, isStorm, Integer.parseInt(args[2]) * 20);
            sender.sendTl(isStorm ? "weatherStormFor" : "weatherSunFor", Worlds.name(world), args[2]);
            return;
        }
        setStorm(world, isStorm, 0);
        sender.sendTl(isStorm ? "weatherStorm" : "weatherSun", Worlds.name(world));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("storm", "sun"));
        } else if (args.length == 2) {
            return COMMON_DURATIONS;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getWorlds(server);
        } else if (args.length == 2) {
            return new ArrayList<>(List.of("storm", "sun"));
        } else if (args.length == 3) {
            return COMMON_DURATIONS;
        } else {
            return Collections.emptyList();
        }
    }
}
