package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandgetpos extends EssentialsCommand {
    public Commandgetpos() {
        super("getpos");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && user.isAuthorized("essentials.getpos.others")) {
            final User otherUser = getPlayer(server, user, args, 0);
            outputPosition(user.getSource(), otherUser.getLocation(), user.getLocation());
            return;
        }
        outputPosition(user.getSource(), user.getLocation(), null);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, args, 0, true, false);
        outputPosition(sender, user.getLocation(), null);
    }

    private void outputPosition(final CommandSource sender, final LazyLocation coords, final LazyLocation distance) {
        sender.sendTl("currentWorld", coords.worldDisplayName(ess.getServer()));
        sender.sendTl("posX", coords.blockX());
        sender.sendTl("posY", coords.blockY());
        sender.sendTl("posZ", coords.blockZ());
        sender.sendTl("posYaw", (coords.yaw() + 360) % 360);
        sender.sendTl("posPitch", coords.pitch());
        if (distance != null && coords.sameWorld(distance)) {
            sender.sendTl("distance", coords.distance(distance));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.getpos.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
