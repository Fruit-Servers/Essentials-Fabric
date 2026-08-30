package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.essentialsx.fabric.utils.NumberUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Commandsethome extends EssentialsCommand {
    public Commandsethome() {
        super("sethome");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, String[] args) throws Exception {
        User usersHome = user;
        String name = "home";
        if (args.length > 0) {
            //Allowing both formats /sethome khobbits house | /sethome khobbits:house
            final String[] nameParts = args[0].split(":");
            if (nameParts[0].length() != args[0].length()) {
                args = nameParts;
            }
            if (args.length < 2) {
                name = args[0].toLowerCase(Locale.ENGLISH);
            } else {
                name = args[1].toLowerCase(Locale.ENGLISH);
                if (user.isAuthorized("essentials.sethome.others")) {
                    usersHome = getPlayer(server, args[0], true, true);
                    if (usersHome == null) {
                        throw new PlayerNotFoundException();
                    }
                }
            }
        }
        if (checkHomeLimit(user, usersHome, name)) {
            name = "home";
        }
        if ("bed".equals(name) || NumberUtil.isInt(name)) {
            throw new TranslatableException("invalidHomeName");
        }
        final LazyLocation location = user.getLocation();
        if ((!ess.getSettings().isTeleportSafetyEnabled() || !ess.getSettings().isForceDisableTeleportSafety()) && LocationUtil.isBlockUnsafeForUser(ess, usersHome, user.getWorld(), location.blockX(), location.blockY(), location.blockZ())) {
            throw new TranslatableException("unsafeTeleportDestination", location.worldDisplayName(server), location.blockX(), location.blockY(), location.blockZ());
        }
        if (ess.getSettings().isConfirmHomeOverwrite() && usersHome.hasHome(name) && (!name.equals(usersHome.getLastHomeConfirmation()) || name.equals(usersHome.getLastHomeConfirmation()) && System.currentTimeMillis() - usersHome.getLastHomeConfirmationTimestamp() > TimeUnit.MINUTES.toMillis(2))) {
            usersHome.setLastHomeConfirmation(name);
            usersHome.setLastHomeConfirmationTimestamp();
            user.sendTl("homeConfirmation", name);
            return;
        }
        usersHome.setHome(name, location);
        user.sendTl("homeSet", location.worldDisplayName(server), location.blockX(), location.blockY(), location.blockZ(), name);
        usersHome.setLastHomeConfirmation(null);
    }

    private boolean checkHomeLimit(final User user, final User usersHome, final String name) throws Exception {
        if (!user.isAuthorized("essentials.sethome.multiple.unlimited")) {
            final int limit = ess.getSettings().getHomeLimit(user);
            if (usersHome.getHomes().size() >= limit) {
                if (usersHome.getHomes().contains(name)) {
                    return false;
                }
                throw new TranslatableException("maxHomes", ess.getSettings().getHomeLimit(user));
            }
            return limit == 1;
        }
        return false;
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        return Collections.emptyList();
    }
}
