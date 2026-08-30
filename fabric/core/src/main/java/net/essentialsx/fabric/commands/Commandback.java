package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandback extends EssentialsCommand {
    public Commandback() {
        super("back");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final CommandSource sender = user.getSource();
        if (args.length > 0 && user.isAuthorized("essentials.back.others")) {
            parseOthers(server, sender, args, commandLabel);
            return;
        }
        teleportBack(sender, user, commandLabel);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        }
        parseOthers(server, sender, args, commandLabel);
    }

    private void parseOthers(final MinecraftServer server, final CommandSource sender, final String[] args, final String commandLabel) throws Exception {
        final User player = getPlayer(server, args, 0, true, false);
        sender.sendTl("backOther", player.getName());
        teleportBack(sender, player, commandLabel);
    }

    private void teleportBack(final CommandSource sender, final User user, final String commandLabel) throws Exception {
        final LazyLocation last = user.getLastLocation();
        if (last == null) {
            throw new TranslatableException("noLocationFound");
        }
        final String lastWorldName = last.level(ess.getServer()) == null ? last.worldDisplayName(ess.getServer()) : Worlds.permissionName(last.level(ess.getServer()));
        User requester = null;
        if (sender.isPlayer()) {
            requester = ess.getUser(sender.getPlayer());
            if (!last.sameWorld(user.getLocation()) && this.ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + lastWorldName)) {
                throw new TranslatableException("noPerm", "essentials.worlds." + lastWorldName);
            }
            if (!requester.isAuthorized("essentials.back.into." + lastWorldName)) {
                throw new TranslatableException("noPerm", "essentials.back.into." + lastWorldName);
            }
        }
        if (requester == null) {
            user.getAsyncTeleport().back(null, null, getNewExceptionFuture(sender, commandLabel));
        } else if (!requester.equals(user)) {
            final Trade charge = new Trade(this.getName(), this.ess);
            charge.isAffordableFor(requester);
            user.getAsyncTeleport().back(requester, charge, getNewExceptionFuture(sender, commandLabel));
        } else {
            final Trade charge = new Trade(this.getName(), this.ess);
            charge.isAffordableFor(user);
            user.getAsyncTeleport().back(charge, getNewExceptionFuture(sender, commandLabel));
        }
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (user.isAuthorized("essentials.back.others") && args.length == 1) {
            return getPlayers(user);
        } else {
            return Collections.emptyList();
        }
    }
}
