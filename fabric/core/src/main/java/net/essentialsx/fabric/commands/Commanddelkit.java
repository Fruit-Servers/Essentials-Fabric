package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.kit.Kit;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commanddelkit extends EssentialsCommand {
    public Commanddelkit() {
        super("delkit");
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            final String kitList = ess.getKits().listKits(ess, null);
            sender.sendTl(kitList.length() > 0 ? "kits" : "noKits", kitList);
            throw new NoChargeException();
        } else {
            final String kitName = ess.getKits().matchKit(args[0]);
            final Kit kit = new Kit(kitName, ess);
            if (sender.getPlayer() != null) {
                kit.checkPerms(ess.getUser(sender.getPlayer()));
            }
            ess.getKits().removeKit(kitName);
            sender.sendTl("deleteKit", kitName);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getKits().getKitKeys());
        }
        return Collections.emptyList();
    }
}
