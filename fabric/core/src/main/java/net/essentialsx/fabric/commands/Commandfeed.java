package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerExemptException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;

public class Commandfeed extends EssentialsLoopCommand {
    public Commandfeed() {
        super("feed");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.isAuthorized("essentials.feed.cooldown.bypass")) {
            user.healCooldown();
        }
        if (args.length > 0 && user.isAuthorized("essentials.feed.others")) {
            loopOnlinePlayers(server, user.getSource(), true, true, args[0], null);
            return;
        }
        feedPlayer(user.getBase());
        user.sendTl("feed");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User player, final String[] args) throws PlayerExemptException {
        feedPlayer(player.getBase());
        sender.sendTl("feedOther", player.getDisplayName());
    }

    private void feedPlayer(final ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10F);
        player.getFoodData().setExhaustion(0F);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.feed.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
