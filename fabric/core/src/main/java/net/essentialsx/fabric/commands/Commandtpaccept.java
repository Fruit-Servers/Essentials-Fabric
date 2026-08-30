package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.AsyncTeleport;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.teleport.TpaRequest;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Commandtpaccept extends EssentialsCommand {
    public Commandtpaccept() {
        super("tpaccept");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final boolean acceptAll;
        if (args.length > 0) {
            acceptAll = args[0].equals("*") || args[0].equalsIgnoreCase("all");
        } else {
            acceptAll = false;
        }
        if (!user.hasPendingTpaRequests(true, acceptAll)) {
            throw new TranslatableException("noPendingRequest");
        }
        if (args.length > 0) {
            if (acceptAll) {
                acceptAllRequests(user, commandLabel);
                throw new NoChargeException();
            }
            user.sendTl("requestAccepted");
            handleTeleport(user, user.getOutstandingTpaRequest(getPlayer(server, user, args, 0).getName(), true), commandLabel);
        } else {
            user.sendTl("requestAccepted");
            handleTeleport(user, user.getNextTpaRequest(true, false, false), commandLabel);
        }
        throw new NoChargeException();
    }

    private void acceptAllRequests(final User user, final String commandLabel) throws Exception {
        TpaRequest request;
        int count = 0;
        while ((request = user.getNextTpaRequest(true, true, true)) != null) {
            try {
                handleTeleport(user, request, commandLabel);
                count++;
            } catch (final Exception e) {
                ess.showError(user.getSource(), e, commandLabel);
            } finally {
                user.removeTpaRequest(request.getName());
            }
        }
        user.sendTl("requestAcceptedAll", count);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(user.getPendingTpaKeys());
            options.add("*");
            return options;
        } else {
            return Collections.emptyList();
        }
    }

    private void handleTeleport(final User user, final TpaRequest request, final String commandLabel) throws Exception {
        if (request == null) {
            throw new TranslatableException("noPendingRequest");
        }
        final User requester = ess.getUser(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            user.removeTpaRequest(request.getName());
            throw new TranslatableException("noPendingRequest");
        }
        if (request.isHere() && ((!requester.isAuthorized("essentials.tpahere") && !requester.isAuthorized("essentials.tpaall")) || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(user.getWorld()))))) {
            throw new TranslatableException("noPendingRequest");
        }
        if (!request.isHere() && (!requester.isAuthorized("essentials.tpa") || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + Worlds.permissionName(requester.getWorld()))))) {
            throw new TranslatableException("noPendingRequest");
        }
        final Trade charge = new Trade(this.getName(), ess);
        requester.sendTl("requestAcceptedFrom", user.getDisplayName());
        final CompletableFuture<Boolean> future = getNewExceptionFuture(requester.getSource(), commandLabel);
        future.exceptionally(e -> {
            user.sendTl("pendingTeleportCancelled");
            return false;
        });
        if (request.isHere()) {
            final LazyLocation loc = request.getLocation();
            final AsyncTeleport teleport = requester.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            future.thenAccept(success -> {
                if (success) {
                    requester.sendTl("teleporting", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
                }
            });
            teleport.teleportPlayer(user, loc, charge, TeleportCause.COMMAND, future);
        } else {
            final AsyncTeleport teleport = requester.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            teleport.teleport(user.getBase(), charge, TeleportCause.COMMAND, future);
        }
        user.removeTpaRequest(request.getName());
    }
}
