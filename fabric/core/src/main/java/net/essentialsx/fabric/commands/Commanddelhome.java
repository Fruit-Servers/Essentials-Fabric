package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commanddelhome extends EssentialsCommand {
    public Commanddelhome() {
        super("delhome");
    }

    private void deleteHome(final CommandSource sender, final User user, final String home) {
        try {
            user.delHome(home);
            sender.sendTl("deleteHome", home);
        } catch (final Exception e) {
            sender.sendTl("invalidHome", home);
        }
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        User user = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        final String name;
        final String[] expandedArg;
        final String[] nameParts = args[0].split(":");
        if (nameParts[0].length() != args[0].length()) {
            expandedArg = nameParts;
        } else {
            expandedArg = args;
        }
        if (expandedArg.length > 1 && (user == null || user.isAuthorized("essentials.delhome.others"))) {
            user = getPlayer(server, expandedArg, 0, true, true);
            name = expandedArg[1].toLowerCase(Locale.ENGLISH);
        } else if (user == null) {
            throw new NotEnoughArgumentsException();
        } else {
            name = expandedArg[0].toLowerCase(Locale.ENGLISH);
        }
        switch (name) {
            case "bed":
                throw new TranslatableException("invalidHomeName");
            case "*":
                final List<String> homes = user.getHomes();
                for (final String home : homes) {
                    deleteHome(sender, user, home);
                }
                break;
            default:
                deleteHome(sender, user, name);
                break;
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        final User user = sender.getUser();
        final boolean canDelOthers = sender.isAuthorized("essentials.delhome.others");
        if (args.length == 1) {
            final List<String> homes = user == null ? new ArrayList<>() : user.getHomes();
            if (canDelOthers) {
                final int sepIndex = args[0].indexOf(':');
                if (sepIndex < 0) {
                    getPlayers(sender).forEach(player -> homes.add(player + ":"));
                } else {
                    final String namePart = args[0].substring(0, sepIndex);
                    final User otherUser;
                    try {
                        otherUser = getPlayer(server, new String[] {namePart}, 0, true, true);
                    } catch (final Exception ex) {
                        return homes;
                    }
                    otherUser.getHomes().forEach(home -> homes.add(namePart + ":" + home));
                    homes.add(namePart + ":" + "*");
                }
            }
            return homes;
        } else {
            return Collections.emptyList();
        }
    }
}
