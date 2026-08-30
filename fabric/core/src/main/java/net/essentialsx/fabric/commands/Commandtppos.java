package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandtppos extends EssentialsCommand {
    public Commandtppos() {
        super("tppos");
    }

    private ServerLevel world(final MinecraftServer server, final User user, final String name, final boolean checkPerm) throws Exception {
        final ServerLevel w = Worlds.get(server, name);
        if (w == null) {
            throw new TranslatableException("invalidWorld");
        }
        if (checkPerm && user.getWorld() != w && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(w))) {
            throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(w));
        }
        return w;
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 3) {
            throw new NotEnoughArgumentsException();
        }
        final double x = Commandtp.relative(args[0], user.getLocation().x());
        final double y = Commandtp.relative(args[1], user.getLocation().y());
        final double z = Commandtp.relative(args[2], user.getLocation().z());
        LazyLocation loc = LazyLocation.of(user.getWorld(), x, y, z, user.getLocation().yaw(), user.getLocation().pitch());
        if (args.length == 4) {
            loc = loc.withLevel(world(server, user, args[3], true));
        }
        if (args.length > 4) {
            loc = loc.withRotation((Float.parseFloat(args[3]) + 360) % 360, Float.parseFloat(args[4]));
        }
        if (args.length > 5) {
            loc = loc.withLevel(world(server, user, args[5], true));
        }
        if (Commandtp.outOfBounds(x, y, z)) {
            throw new NotEnoughArgumentsException(user.playerTl("teleportInvalidLocation"));
        }
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        user.sendTl("teleporting", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
        user.getAsyncTeleport().teleport(loc, charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
        throw new NoChargeException();
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 4) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, args, 0, true, false);
        final double x = Commandtp.relative(args[1], user.getLocation().x());
        final double y = Commandtp.relative(args[2], user.getLocation().y());
        final double z = Commandtp.relative(args[3], user.getLocation().z());
        LazyLocation loc = LazyLocation.of(user.getWorld(), x, y, z, user.getLocation().yaw(), user.getLocation().pitch());
        if (args.length == 5) {
            loc = loc.withLevel(world(server, user, args[4], false));
        }
        if (args.length > 5) {
            loc = loc.withRotation((Float.parseFloat(args[4]) + 360) % 360, Float.parseFloat(args[5]));
        }
        if (args.length > 6) {
            loc = loc.withLevel(world(server, user, args[6], false));
        }
        if (Commandtp.outOfBounds(x, y, z)) {
            throw new NotEnoughArgumentsException(sender.tl("teleportInvalidLocation"));
        }
        sender.sendTl("teleporting", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
        user.sendTl("teleporting", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
        user.getAsyncTeleport().teleport(loc, null, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 || args.length == 2 || args.length == 3) {
            return new ArrayList<>(List.of("~0"));
        } else if (args.length == 4 || args.length == 5) {
            return new ArrayList<>(List.of("0"));
        } else if (args.length == 6) {
            return getWorlds(server);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else if (args.length == 2 || args.length == 3 || args.length == 4) {
            return new ArrayList<>(List.of("~0"));
        } else if (args.length == 5 || args.length == 6) {
            return new ArrayList<>(List.of("0"));
        } else if (args.length == 7) {
            return getWorlds(server);
        } else {
            return Collections.emptyList();
        }
    }
}
