package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.LocationUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// This method contains an undocumented sub command #EasterEgg
public class Commandjump extends EssentialsCommand {
    public Commandjump() {
        super("jump");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && args[0].contains("lock") && user.isAuthorized("essentials.jump.lock")) {
            if (user.isFlyClickJump()) {
                user.setRightClickJump(false);
                user.sendTl("jumpEasterDisable");
            } else {
                user.setRightClickJump(true);
                user.sendTl("jumpEasterEnable");
            }
            return;
        }
        final LazyLocation loc;
        final LazyLocation cloc = user.getLocation();
        try {
            final LazyLocation target = LocationUtil.getTarget(user.getBase());
            if (target == null) {
                throw new NullPointerException();
            }
            loc = target.withRotation(cloc.yaw(), cloc.pitch()).add(0, 1, 0);
        } catch (final NullPointerException ex) {
            throw new TranslatableException(ex, "jumpError");
        }
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        user.getAsyncTeleport().teleport(loc, charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.jump.lock")) {
            return new ArrayList<>(List.of("lock", "unlock"));
        } else {
            return Collections.emptyList();
        }
    }
}
