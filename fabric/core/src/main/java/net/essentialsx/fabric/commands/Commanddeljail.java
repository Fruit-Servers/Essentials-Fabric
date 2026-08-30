package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commanddeljail extends EssentialsCommand {
    public Commanddeljail() {
        super("deljail");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        if (!ess.getJails().getList().contains(args[0].toLowerCase(Locale.ENGLISH))) {
            throw new TranslatableException("jailNotExist");
        }
        ess.getJails().removeJail(args[0]);
        sender.sendTl("deleteJail", args[0]);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getJails().getList());
        } else {
            return Collections.emptyList();
        }
    }
}
