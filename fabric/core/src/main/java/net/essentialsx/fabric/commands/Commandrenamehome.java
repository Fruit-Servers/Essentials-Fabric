package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandrenamehome extends EssentialsCommand {
    public Commandrenamehome() {
        super("renamehome");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        User usersHome = user;
        final String oldName;
        final String newName;
        // Allowing both formats /renamehome jroy home1 home | /sethome jroy:home1 home
        if (args.length == 2) {
            final String[] nameParts = args[0].split(":", 2);
            newName = args[1].toLowerCase(Locale.ENGLISH);
            if (nameParts.length == 2) {
                oldName = nameParts[1].toLowerCase(Locale.ENGLISH);
                if (user.isAuthorized("essentials.renamehome.others")) {
                    usersHome = getPlayer(server, nameParts[0], true, true);
                    if (usersHome == null) {
                        throw new PlayerNotFoundException();
                    }
                }
            } else {
                oldName = args[0].toLowerCase(Locale.ENGLISH);
            }
        } else if (args.length == 3) {
            if (!user.isAuthorized("essentials.renamehome.others")) {
                throw new NotEnoughArgumentsException();
            }
            usersHome = getPlayer(server, args[0], true, true);
            if (usersHome == null) {
                throw new PlayerNotFoundException();
            }
            oldName = args[1].toLowerCase(Locale.ENGLISH);
            newName = args[2].toLowerCase(Locale.ENGLISH);
        } else {
            throw new NotEnoughArgumentsException();
        }
        if ("bed".equals(newName) || NumberUtil.isInt(newName) || "bed".equals(oldName) || NumberUtil.isInt(oldName)) {
            throw new TranslatableException("invalidHomeName");
        }
        usersHome.renameHome(oldName, newName);
        user.sendTl("homeRenamed", oldName, newName);
        usersHome.setLastHomeConfirmation(null);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        final User user = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        if (args.length != 1) {
            return Collections.emptyList();
        }
        final List<String> homes = user == null ? new ArrayList<>() : user.getHomes();
        final boolean canRenameOthers = sender.isAuthorized("essentials.renamehome.others");
        if (canRenameOthers) {
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
            }
        }
        return homes;
    }
}
