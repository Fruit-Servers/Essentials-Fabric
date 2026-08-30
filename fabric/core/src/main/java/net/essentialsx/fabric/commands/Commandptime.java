package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.listener.PlayerTimeWeather;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DescParseTickFormat;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class Commandptime extends EssentialsLoopCommand {
    private static final List<String> GET_ALIASES = Arrays.asList("get", "list", "show", "display");

    public Commandptime() {
        super("ptime");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 || GET_ALIASES.contains(args[0].toLowerCase(Locale.ENGLISH))) {
            if (args.length > 1) { // /ptime get md_5 || /ptime get *
                if (args[1].equals("*") || args[1].equals("**")) {
                    sender.sendTl("pTimePlayers");
                }
                loopOnlinePlayersConsumer(server, sender, false, true, args[1], player -> getUserTime(sender, player));
                return;
            }
            if (args.length == 1 || sender.isPlayer()) { // /ptime get
                if (sender.isPlayer()) {
                    getUserTime(sender, sender.getUser());
                    return;
                }
                throw new NotEnoughArgumentsException();
            }
            if (ess.getOnlinePlayers().size() > 1) {
                sender.sendTl("pTimePlayers");
            }
            for (final User player : ess.getOnlineUsers()) {
                getUserTime(sender, player);
            }
            return;
        }
        if (args.length > 1 && !sender.isAuthorized("essentials.ptime.others") && !args[1].equalsIgnoreCase(sender.getSelfSelector())) {
            sender.sendTl("pTimeOthersPermission");
            return;
        }
        String time = args[0];
        final boolean fixed = time.startsWith("@");
        if (fixed) {
            time = time.substring(1);
        }
        final Long ticks;
        if (DescParseTickFormat.meansReset(time)) {
            ticks = null;
        } else {
            try {
                ticks = DescParseTickFormat.parse(time);
            } catch (final NumberFormatException e) {
                throw new NotEnoughArgumentsException(e);
            }
        }
        final StringJoiner joiner = new StringJoiner(", ");
        loopOnlinePlayersConsumer(server, sender, false, true, args.length > 1 ? args[1] : sender.getSelfSelector(), player -> {
            setUserTime(player, ticks, !fixed);
            joiner.add(player.getName());
        });
        if (ticks == null) {
            sender.sendTl("pTimeReset", joiner.toString());
            return;
        }
        final String formattedTime = DescParseTickFormat.format(ticks);
        sender.sendTl(fixed ? "pTimeSetFixed" : "pTimeSet", Text.parsed(formattedTime), joiner.toString());
    }

    public void getUserTime(final CommandSource sender, final IUser user) {
        if (user == null) {
            return;
        }
        final PlayerTimeWeather ptw = ess.getPlayerTimeWeather();
        if (!ptw.hasPlayerTime(user.getUUID())) {
            sender.sendTl("pTimeNormal", user.getName());
            return;
        }
        final String time = DescParseTickFormat.format(ptw.getPlayerTime(user.getBase()));
        sender.sendTl(ptw.isPlayerTimeRelative(user.getUUID()) ? "pTimeCurrent" : "pTimeCurrentFixed", user.getName(), Text.parsed(time));
    }

    private void setUserTime(final User user, final Long ticks, final boolean relative) {
        final PlayerTimeWeather ptw = ess.getPlayerTimeWeather();
        if (ticks == null) {
            ptw.resetPlayerTime(user.getBase());
        } else {
            long time = ptw.getPlayerTime(user.getBase());
            time -= time % 24000;
            time += 24000 + ticks;
            if (relative) {
                time -= user.getWorld().getDayTime();
            }
            ptw.setPlayerTime(user.getBase(), time, relative);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        final User user = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        if (args.length == 1) {
            return new ArrayList<>(List.of("get", "reset", "sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight"));
        } else if (args.length == 2 && (GET_ALIASES.contains(args[0]) || user == null || user.isAuthorized("essentials.ptime.others"))) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
    }
}
