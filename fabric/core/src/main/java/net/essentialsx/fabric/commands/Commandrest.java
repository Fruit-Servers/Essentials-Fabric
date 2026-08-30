package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerExemptException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;

import java.util.Collections;
import java.util.List;

public class Commandrest extends EssentialsLoopCommand {
    public Commandrest() {
        super("rest");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 && !sender.isPlayer()) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length > 0 && sender.isAuthorized("essentials.rest.others")) {
            loopOnlinePlayers(server, sender, false, true, args[0], null);
            return;
        }
        restPlayer(sender.getUser());
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User player, final String[] args) throws PlayerExemptException {
        restPlayer(player);
        sender.sendTl("restOther", player.getDisplayName());
    }

    private void restPlayer(final IUser user) {
        user.getBase().getStats().setValue(user.getBase(), Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 0);
        user.sendTl("rest");
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.rest.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
