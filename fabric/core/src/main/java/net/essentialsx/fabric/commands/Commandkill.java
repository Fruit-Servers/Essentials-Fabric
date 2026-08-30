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

public class Commandkill extends EssentialsLoopCommand {
    public Commandkill() {
        super("kill");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) throws PlayerExemptException {
        final ServerPlayer matchPlayer = user.getBase();
        if (sender.isPlayer() && user.isAuthorized("essentials.kill.exempt") && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.kill.force")) {
            throw new PlayerExemptException("killExempt", user.getDisplayName());
        }
        // Generic kill damage bypasses god mode/invulnerability like upstream's CUSTOM cause.
        matchPlayer.kill();
        if (!matchPlayer.isDeadOrDying()) {
            matchPlayer.setHealth(0);
        }
        sender.sendTl("kill", user.getDisplayName());
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
