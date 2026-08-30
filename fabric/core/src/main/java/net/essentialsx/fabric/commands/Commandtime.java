package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.listener.PlayerTimeWeather;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.utils.DescParseTickFormat;
import net.essentialsx.fabric.utils.NumberUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

public class Commandtime extends EssentialsCommand {
    private static final List<String> SUB_COMMANDS = Arrays.asList("add", "set");
    private static final List<String> TIME_NAMES = Arrays.asList("sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight");
    private static final List<String> TIME_NUMBERS = Arrays.asList("1000", "2000", "3000", "4000", "5000");

    public Commandtime() {
        super("time");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final long timeTick;
        final Set<ServerLevel> worlds;
        boolean add = false;
        if (args.length == 0) {
            worlds = getWorlds(server, sender, null);
            if (commandLabel.endsWith("day") || commandLabel.endsWith("night")) {
                timeTick = DescParseTickFormat.parse(commandLabel.toLowerCase(Locale.ENGLISH).replace("e", ""));
            } else {
                getWorldsTime(sender, worlds);
                return;
            }
        } else if (args.length == 1) {
            worlds = getWorlds(server, sender, null);
            try {
                timeTick = DescParseTickFormat.parse(NumberUtil.isInt(args[0]) ? (args[0] + "t") : args[0]);
            } catch (final NumberFormatException e) {
                throw new NotEnoughArgumentsException(e);
            }
        } else {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                try {
                    add = args[0].equalsIgnoreCase("add");
                    timeTick = DescParseTickFormat.parse(NumberUtil.isInt(args[1]) ? (args[1] + "t") : args[1]);
                    worlds = getWorlds(server, sender, args.length > 2 ? args[2] : null);
                } catch (final NumberFormatException e) {
                    throw new NotEnoughArgumentsException(e);
                }
            } else {
                try {
                    timeTick = DescParseTickFormat.parse(NumberUtil.isInt(args[0]) ? (args[0] + "t") : args[0]);
                    worlds = getWorlds(server, sender, args[1]);
                } catch (final NumberFormatException e) {
                    throw new NotEnoughArgumentsException(e);
                }
            }
        }
        // Start updating world times, we have what we need
        if (!sender.isAuthorized("essentials.time.set")) {
            throw new TranslatableException("timeSetPermission");
        }
        for (final ServerLevel world : worlds) {
            if (!canUpdateWorld(sender, world)) {
                throw new TranslatableException("timeSetWorldPermission", Worlds.name(sender.getPlayer().serverLevel()));
            }
        }
        final PlayerTimeWeather ptw = ess.getPlayerTimeWeather();
        final StringJoiner joiner = new StringJoiner(", ");
        for (final ServerLevel world : worlds) {
            // Capture intended visible time for players with relative ptime before world time changes
            final Map<ServerPlayer, Long> ptimePlayers = new HashMap<>();
            for (final ServerPlayer player : world.players()) {
                if (ptw.hasPlayerTime(player.getUUID()) && ptw.isPlayerTimeRelative(player.getUUID())) {
                    ptimePlayers.put(player, ptw.getPlayerTime(player));
                }
            }
            long time = world.getDayTime();
            if (!add) {
                time -= time % 24000;
            }
            world.setDayTime(time + (add ? 0 : 24000) + timeTick);
            // Re-apply ptime offsets so players maintain their intended visible time
            final long newWorldTime = world.getDayTime();
            for (final Map.Entry<ServerPlayer, Long> entry : ptimePlayers.entrySet()) {
                ptw.setPlayerTime(entry.getKey(), entry.getValue() - newWorldTime, true);
            }
            joiner.add(Worlds.name(world));
        }
        sender.sendTl(add ? "timeWorldAdd" : "timeWorldSet", DescParseTickFormat.formatTicks(timeTick), joiner.toString());
    }

    private void getWorldsTime(final CommandSource sender, final Collection<ServerLevel> worlds) {
        if (worlds.size() == 1) {
            final Iterator<ServerLevel> iter = worlds.iterator();
            sender.sendComponent(Text.get().deserializeMiniMessage(DescParseTickFormat.format(iter.next().getDayTime())));
            return;
        }
        for (final ServerLevel world : worlds) {
            sender.sendTl("timeWorldCurrent", Worlds.name(world), Text.parsed(DescParseTickFormat.format(world.getDayTime())));
        }
    }

    /**
     * Parses worlds from command args, otherwise returns all worlds.
     */
    private Set<ServerLevel> getWorlds(final MinecraftServer server, final CommandSource sender, final String selector) throws Exception {
        final Set<ServerLevel> worlds = new TreeSet<>(Comparator.comparing(Worlds::name));
        // If there is no selector we want the world the user is currently in. Or all worlds if it isn't a user.
        if (selector == null) {
            if (sender.isPlayer()) {
                worlds.add(sender.getPlayer().serverLevel());
            } else {
                worlds.addAll(Worlds.all(server));
            }
            return worlds;
        }
        // Try to find the world with name = selector
        final ServerLevel world = Worlds.get(server, selector);
        if (world != null) {
            worlds.add(world);
        } else if (selector.equalsIgnoreCase("*") || selector.equalsIgnoreCase("all")) {
            worlds.addAll(Worlds.all(server));
        } else {
            throw new TranslatableException("invalidWorld");
        }
        return worlds;
    }

    private boolean canUpdateAll(final CommandSource sender) {
        return !ess.getSettings().isWorldTimePermissions() || sender.isAuthorized("essentials.time.world.all");
    }

    private boolean canUpdateWorld(final CommandSource sender, final ServerLevel world) {
        return canUpdateAll(sender) || sender.isAuthorized("essentials.time.world." + normalizeWorldName(world));
    }

    private String normalizeWorldName(final ServerLevel world) {
        return Worlds.name(world).toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "_");
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            if (sender.isAuthorized("essentials.time.set")) {
                return SUB_COMMANDS;
            } else {
                return Collections.emptyList();
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                return TIME_NAMES;
            } else if (args[0].equalsIgnoreCase("add")) {
                return TIME_NUMBERS;
            } else {
                return Collections.emptyList();
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            final List<String> worlds = new ArrayList<>();
            for (final ServerLevel world : Worlds.all(server)) {
                if (sender.isAuthorized("essentials.time.world." + normalizeWorldName(world))) {
                    worlds.add(Worlds.name(world));
                }
            }
            if (sender.isAuthorized("essentials.time.world.all")) {
                worlds.add("*");
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }
}
