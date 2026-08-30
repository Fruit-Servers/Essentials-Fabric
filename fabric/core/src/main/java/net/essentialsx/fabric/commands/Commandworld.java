package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
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

public class Commandworld extends EssentialsCommand {
    public Commandworld() {
        super("world");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ServerLevel world;
        final List<ServerLevel> worlds = Worlds.all(server);
        if (args.length < 1) {
            ServerLevel nether = null;
            for (final ServerLevel world2 : worlds) {
                if (Worlds.isNether(world2)) {
                    nether = world2;
                    break;
                }
            }
            if (nether == null) {
                return;
            }
            world = user.getWorld() == nether ? worlds.get(0) : nether;
        } else {
            world = Worlds.get(server, getFinalArg(args, 0));
            if (world == null) {
                user.sendTl("invalidWorld");
                user.sendTl("possibleWorlds", worlds.size() - 1);
                user.sendTl("typeWorldName");
                throw new NoChargeException();
            }
        }
        if (ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(world))) {
            throw new TranslatableException("noPerm", "essentials.worlds." + Worlds.permissionName(world));
        }
        final double factor;
        if (Worlds.isNether(user.getWorld()) && Worlds.isOverworldLike(world)) {
            factor = 8.0;
        } else if (Worlds.isOverworldLike(user.getWorld()) && Worlds.isNether(world)) {
            factor = 1.0 / 8.0;
        } else {
            factor = 1.0;
        }
        final LazyLocation loc = user.getLocation();
        final LazyLocation target = LazyLocation.of(world, loc.blockX() * factor + .5, loc.blockY(), loc.blockZ() * factor + .5, loc.yaw(), loc.pitch());
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        user.getAsyncTeleport().teleport(target, charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> worlds = new ArrayList<>();
            for (final ServerLevel world : Worlds.all(server)) {
                if (ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(world))) {
                    continue;
                }
                worlds.add(Worlds.name(world));
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }
}
