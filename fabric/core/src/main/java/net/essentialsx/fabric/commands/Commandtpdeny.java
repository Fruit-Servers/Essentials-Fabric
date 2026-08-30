package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.teleport.TpaRequest;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandtpdeny extends EssentialsCommand {
    public Commandtpdeny() {
        super("tpdeny");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final boolean denyAll;
        if (args.length > 0) {
            denyAll = args[0].equals("*") || args[0].equalsIgnoreCase("all");
        } else {
            denyAll = false;
        }
        if (!user.hasPendingTpaRequests(false, false)) {
            throw new TranslatableException("noPendingRequest");
        }
        final TpaRequest denyRequest;
        if (args.length > 0) {
            if (denyAll) {
                denyAllRequests(user);
                return;
            }
            denyRequest = user.getOutstandingTpaRequest(getPlayer(server, user, args, 0).getName(), false);
        } else {
            denyRequest = user.getNextTpaRequest(false, true, false);
        }
        if (denyRequest == null) {
            throw new TranslatableException("noPendingRequest");
        }
        final User player = ess.getUser(denyRequest.getRequesterUuid());
        if (player == null || !player.isOnline()) {
            throw new TranslatableException("noPendingRequest");
        }
        user.sendTl("requestDenied");
        player.sendTl("requestDeniedFrom", user.getDisplayName());
        user.removeTpaRequest(denyRequest.getName());
    }

    private void denyAllRequests(final User user) {
        TpaRequest request;
        int count = 0;
        while ((request = user.getNextTpaRequest(false, true, false)) != null) {
            final User player = ess.getUser(request.getRequesterUuid());
            if (player != null && player.isOnline()) {
                player.sendTl("requestDeniedFrom", user.getDisplayName());
            }
            user.removeTpaRequest(request.getName());
            count++;
        }
        user.sendTl("requestDeniedAll", count);
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
}
