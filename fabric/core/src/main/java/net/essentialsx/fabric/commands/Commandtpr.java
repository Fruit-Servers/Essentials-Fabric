package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.rtp.RandomTeleport;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Commandtpr extends EssentialsCommand {
    public Commandtpr() {
        super("tpr");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        final String randomLocationName;
        final User target;
        if (args.length == 0) {
            randomLocationName = randomTeleport.getDefaultLocation().replace("{world}", Worlds.name(user.getWorld()));
            target = user;
        } else {
            randomLocationName = args[0];
            if (!randomTeleport.hasLocation(randomLocationName)) {
                throw new TranslatableException("tprNotExist");
            }
            if (randomTeleport.isPerLocationPermission() && !user.isAuthorized("essentials.tpr.location." + randomLocationName)) {
                throw new TranslatableException("tprNoPermission");
            }
            if (args.length > 1 && user.isAuthorized("essentials.tpr.others")) {
                target = getPlayer(server, user, args, 1);
            } else {
                target = user;
            }
        }
        target.sendTl("tprSuccess");
        if (target != user) {
            user.sendTl("tprOtherUser", target.getDisplayName());
        }
        randomTeleport.getRandomLocation(randomLocationName).thenAccept(location -> ess.scheduleSyncDelayedTask(() -> {
            final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
            future.thenAccept(success -> {
                if (success) {
                    target.sendTl("tprSuccessDone");
                }
            });
            target.getAsyncTeleport().teleport(location, charge, TeleportCause.COMMAND, future);
        }));
        throw new NoChargeException();
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        final User userToTeleport = getPlayer(server, sender, args, 1);
        final String potentialLocation = args[0];
        if (!randomTeleport.hasLocation(potentialLocation)) {
            throw new TranslatableException("tprNotExist");
        }
        userToTeleport.sendTl("tprSuccess");
        sender.sendTl("tprOtherUser", userToTeleport.getDisplayName());
        randomTeleport.getRandomLocation(potentialLocation).thenAccept(location -> ess.scheduleSyncDelayedTask(() -> {
            final CompletableFuture<Boolean> future = getNewExceptionFuture(sender, commandLabel);
            future.thenAccept(success -> {
                if (success) {
                    userToTeleport.sendTl("tprSuccessDone");
                }
            });
            userToTeleport.getAsyncTeleport().now(location, false, TeleportCause.COMMAND, future);
        }));
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        if (args.length == 1) {
            if (randomTeleport.isPerLocationPermission()) {
                return randomTeleport.listLocations().stream().filter(name -> sender.isAuthorized("essentials.tpr.location." + name)).collect(Collectors.toList());
            } else {
                return randomTeleport.listLocations();
            }
        } else if (args.length == 2 && sender.isAuthorized("essentials.tpr.others")) {
            return getPlayers(sender);
        }
        return Collections.emptyList();
    }
}
