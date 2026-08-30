package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandthunder extends EssentialsCommand {
    public Commandthunder() {
        super("thunder");
    }

    /** Sets thundering on a level, keeping the current rain state (mirrors Bukkit World#setThundering). */
    public static void setThundering(final ServerLevel world, final boolean thunder, final int durationTicks) {
        final boolean raining = world.isRaining() || thunder;
        final int duration = durationTicks > 0 ? durationTicks : (thunder ? 6000 : 0);
        world.setWeatherParameters(thunder ? 0 : duration, thunder ? duration : 0, raining, thunder);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final ServerLevel world = user.getWorld();
        final boolean setThunder = args[0].equalsIgnoreCase("true");
        if (args.length == 1) {
            setThundering(world, setThunder, 0);
            user.sendTl("thunder", CommonPlaceholders.enableDisable(user.getSource(), setThunder));
            return;
        }
        setThundering(world, setThunder, Integer.parseInt(args[1]) * 20);
        user.sendTl("thunderDuration", CommonPlaceholders.enableDisable(user.getSource(), setThunder), Integer.parseInt(args[1]));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("true", "false"));
        } else if (args.length == 2) {
            return COMMON_DATE_DIFFS;
        } else {
            return Collections.emptyList();
        }
    }
}
