package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.user.LazyLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandwarpinfo extends EssentialsCommand {
    public Commandwarpinfo() {
        super("warpinfo");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        final String name = args[0];
        final LazyLocation loc = ess.getWarps().getWarp(name);
        sender.sendTl("warpInfo", name);
        sender.sendTl("whoisLocation", loc.worldDisplayName(server), loc.blockX(), loc.blockY(), loc.blockZ());
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            if (ess.getSettings().getPerWarpPermission() && sender.isPlayer()) {
                final List<String> list = new ArrayList<>();
                for (final String curWarp : ess.getWarps().getList()) {
                    if (sender.isAuthorized("essentials.warps." + curWarp)) {
                        list.add(curWarp);
                    }
                }
                return list;
            }
            return new ArrayList<>(ess.getWarps().getList());
        } else {
            return Collections.emptyList();
        }
    }
}
