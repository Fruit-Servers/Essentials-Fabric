package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class Commandjailedplayers extends EssentialsCommand {
    public Commandjailedplayers() {
        super("jailedplayers");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final List<ServerPlayer> jailed = ess.getJailedPlayers();
        if (jailed != null && !jailed.isEmpty()) {
            sender.sendTl("jailedPlayersList", StringUtil.joinList(", ", jailed.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.toList()).toArray()));
        } else {
            sender.sendTl("jailedPlayersEmpty");
        }
    }
}
