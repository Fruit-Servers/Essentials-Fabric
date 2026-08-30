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

public class Commandkitreset extends EssentialsCommand {
    public Commandkitreset() {
        super("kitreset");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final String kitName = args[0];
        if (ess.getKits().getKit(kitName) == null) {
            throw new TranslatableException("kitNotFound");
        }
        User target = user;
        if (args.length > 1 && user.isAuthorized("essentials.kitreset.others")) {
            target = getPlayer(server, user, args, 1);
        }
        target.setKitTimestamp(kitName, 0);
        if (user.equals(target)) {
            user.sendTl("kitReset", kitName);
        } else {
            user.sendTl("kitResetOther", kitName, target.getDisplayName());
        }
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final String kitName = args[0];
        if (ess.getKits().getKit(kitName) == null) {
            throw new TranslatableException("kitNotFound");
        }
        final User target = getPlayer(server, sender, args, 1);
        target.setKitTimestamp(kitName, 0);
        sender.sendTl("kitResetOther", kitName, target.getDisplayName());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getKits().getKitKeys());
        } else if (args.length == 2 && sender.isAuthorized("essentials.kitreset.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
