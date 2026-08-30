package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;

public class Commandext extends EssentialsLoopCommand {
    public Commandext() {
        super("ext");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && user.isAuthorized("essentials.ext.others")) {
            loopOnlinePlayers(server, user.getSource(), true, true, args[0], null);
            return;
        }
        extPlayer(user.getBase());
        user.sendTl("extinguish");
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User player, final String[] args) {
        extPlayer(player.getBase());
        sender.sendTl("extinguishOthers", player.getDisplayName());
    }

    private void extPlayer(final ServerPlayer player) {
        player.clearFire();
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
