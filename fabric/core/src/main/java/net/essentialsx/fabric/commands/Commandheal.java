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

public class Commandheal extends EssentialsLoopCommand {
    public Commandheal() {
        super("heal");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.isAuthorized("essentials.heal.cooldown.bypass")) {
            user.healCooldown();
        }
        if (args.length > 0 && user.isAuthorized("essentials.heal.others")) {
            loopOnlinePlayers(server, user.getSource(), true, true, args[0], null);
            return;
        }
        updatePlayer(server, user.getSource(), user, args);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User user, final String[] args) throws PlayerExemptException {
        final ServerPlayer player = user.getBase();
        if (player.getHealth() == 0) {
            throw new PlayerExemptException("healDead");
        }
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        user.sendTl("heal");
        if (ess.getSettings().isRemovingEffectsOnHeal()) {
            player.removeAllEffects();
        }
        if (!sender.isPlayer() || !user.getBase().equals(sender.getPlayer())) {
            sender.sendTl("healOther", user.getDisplayName());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.heal.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
