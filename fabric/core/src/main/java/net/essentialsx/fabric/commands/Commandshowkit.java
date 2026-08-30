package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.kit.Kit;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandshowkit extends EssentialsCommand {
    public Commandshowkit() {
        super("showkit");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length != 1) {
            throw new NotEnoughArgumentsException();
        }
        for (final String kitName : args[0].toLowerCase(Locale.ENGLISH).split(",")) {
            user.sendTl("kitContains", kitName);
            for (final String s : new Kit(kitName, ess).getItems()) {
                user.sendTl("kitItem", s);
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getKits().getKitKeys());
        } else {
            return Collections.emptyList();
        }
    }
}
