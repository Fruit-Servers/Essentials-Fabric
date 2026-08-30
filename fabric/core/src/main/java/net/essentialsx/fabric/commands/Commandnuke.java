package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandnuke extends EssentialsCommand {
    /** Scoreboard tag marking Essentials-spawned nuke TNT (listeners can exempt it from block damage). */
    public static final String NUKE_TAG = "ess_tnt_proj";

    public Commandnuke() {
        super("nuke");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws NotEnoughArgumentsException, PlayerNotFoundException {
        final Iterable<User> targets;
        if (args.length > 0) {
            targets = new ArrayList<>();
            for (int i = 0; i < args.length; ++i) {
                ((ArrayList<User>) targets).add(getPlayer(server, sender, args, i));
            }
        } else {
            targets = ess.getOnlineUsers();
        }
        for (final User user : targets) {
            if (user == null || !user.isOnline()) {
                continue;
            }
            user.sendTl("nuke");
            final LazyLocation loc = user.getLocation();
            final ServerLevel world = user.getWorld();
            if (world != null) {
                for (int x = -10; x <= 10; x += 5) {
                    for (int z = -10; z <= 10; z += 5) {
                        final int y = LocationUtil.getHighestBlockY(world, loc.blockX(), loc.blockZ()) + 64;
                        final PrimedTnt entity = new PrimedTnt(world, loc.blockX() + x + 0.5, y, loc.blockZ() + z + 0.5, null);
                        entity.addTag(NUKE_TAG);
                        world.addFreshEntity(entity);
                    }
                }
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
