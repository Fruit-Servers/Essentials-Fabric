package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandice extends EssentialsLoopCommand {
    public Commandice() {
        super("ice");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0 && !sender.isPlayer()) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length > 0 && sender.isAuthorized("essentials.ice.others")) {
            loopOnlinePlayers(server, sender, false, true, args[0], null);
            return;
        }
        freezePlayer(sender.getUser());
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) {
        freezePlayer(user);
        sender.sendTl("iceOther", user.getDisplayName());
    }

    private void freezePlayer(final IUser user) {
        user.getBase().setTicksFrozen(user.getBase().getTicksRequiredToFreeze());
        user.sendTl("ice");
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.ice.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
