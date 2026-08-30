package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.economy.Trade;
import net.essentialsx.fabric.teleport.TeleportCause;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.StringUtil;
import net.essentialsx.fabric.utils.Worlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Commandhome extends EssentialsCommand {
    public Commandhome() {
        super("home");
    }

    /** The player's bed/anchor location, or null when none is set or the block is missing. */
    public static LazyLocation bedLocation(final MinecraftServer server, final ServerPlayer player) {
        if (player.getRespawnPosition() == null || server.getLevel(player.getRespawnDimension()) == null) {
            return null;
        }
        final DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        if (transition.missingRespawnBlock()) {
            return null;
        }
        return LazyLocation.of(transition.newLevel(), transition.pos().x, transition.pos().y, transition.pos().z, transition.yRot(), transition.xRot());
    }

    // This method contains an undocumented translation parameters #EasterEgg
    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        User player = user;
        String homeName = "";
        final String[] nameParts;
        if (args.length > 0) {
            nameParts = args[0].split(":");
            if (nameParts[0].length() == args[0].length() || !user.isAuthorized("essentials.home.others")) {
                homeName = nameParts[0];
            } else {
                player = getPlayer(server, nameParts, 0, true, true);
                if (nameParts.length > 1) {
                    homeName = nameParts[1];
                }
            }
        }
        try {
            if ("bed".equalsIgnoreCase(homeName)) {
                if (!user.isAuthorized("essentials.home.bed")) {
                    throw new TranslatableException("noAccessCommand");
                }
                if (!player.isOnline()) {
                    throw new TranslatableException("bedOffline");
                }
                final LazyLocation location = bedLocation(server, player.getBase());
                final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
                if (location != null) {
                    future.thenAccept(success -> {
                        if (success) {
                            user.sendTl("teleportHome", "bed");
                        }
                    });
                    user.getAsyncTeleport().teleport(location, charge, TeleportCause.COMMAND, future);
                } else {
                    showError(user.getSource(), new TranslatableException("bedMissing"), commandLabel);
                }
                throw new NoChargeException();
            }
            goHome(user, player, homeName.toLowerCase(Locale.ENGLISH), charge, getNewExceptionFuture(user.getSource(), commandLabel));
        } catch (final NotEnoughArgumentsException e) {
            final User finalPlayer = player;
            final LazyLocation bed = finalPlayer.isOnline() ? bedLocation(server, finalPlayer.getBase()) : null;
            final List<String> homes = finalPlayer.getHomes();
            if (homes.isEmpty() && finalPlayer.equals(user)) {
                if (ess.getSettings().isSpawnIfNoHome()) {
                    final boolean useBed = bed != null && user.isAuthorized("essentials.home.bed");
                    if (useBed) {
                        user.getAsyncTeleport().respawn(charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
                    } else {
                        user.getAsyncTeleport().teleport(ess.getWorldSpawn(finalPlayer.getWorld()), charge, TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
                    }
                } else {
                    showError(user.getSource(), new TranslatableException("noHomeSetPlayer"), commandLabel);
                }
            } else if (homes.isEmpty() || !finalPlayer.hasValidHomes()) {
                showError(user.getSource(), new TranslatableException("noHomeSetPlayer"), commandLabel);
            } else if (homes.size() == 1 && finalPlayer.equals(user)) {
                try {
                    goHome(user, finalPlayer, homes.get(0), charge, getNewExceptionFuture(user.getSource(), commandLabel));
                } catch (final Exception exception) {
                    showError(user.getSource(), exception, commandLabel);
                }
            } else {
                final int count = homes.size();
                if (user.isAuthorized("essentials.home.bed")) {
                    if (bed != null) {
                        homes.add(user.playerTl("bed"));
                    } else {
                        homes.add(user.playerTl("bedNull"));
                    }
                }
                user.sendTl("homes", Text.parsed(StringUtil.joinList(homes)), count, getHomeLimit(finalPlayer));
            }
        }
        throw new NoChargeException();
    }

    private String getHomeLimit(final User player) {
        if (!player.isOnline()) {
            return "?";
        }
        if (player.isAuthorized("essentials.sethome.multiple.unlimited")) {
            return "*";
        }
        return Integer.toString(ess.getSettings().getHomeLimit(player));
    }

    private void goHome(final User user, final User player, final String home, final Trade charge, final CompletableFuture<Boolean> future) throws Exception {
        if (home.length() < 1) {
            throw new NotEnoughArgumentsException();
        }
        final LazyLocation loc = player.getHome(home);
        if (loc == null) {
            throw new NotEnoughArgumentsException();
        }
        final String worldName = loc.level(ess.getServer()) == null ? loc.worldDisplayName(ess.getServer()) : Worlds.permissionName(loc.level(ess.getServer()));
        if (!loc.sameWorld(user.getLocation()) && ess.getSettings().isWorldHomePermissions() && !user.isAuthorized("essentials.worlds." + worldName)) {
            throw new TranslatableException("noPerm", "essentials.worlds." + worldName);
        }
        user.getAsyncTeleport().teleport(loc, charge, TeleportCause.COMMAND, future);
        future.thenAccept(success -> {
            if (success) {
                user.sendTl("teleportHome", home);
            }
        });
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final User user, final String commandLabel, final String[] args) {
        final boolean canVisitOthers = user.isAuthorized("essentials.home.others");
        final boolean canVisitBed = user.isAuthorized("essentials.home.bed");
        if (args.length == 1) {
            final List<String> homes = user.getHomes();
            if (canVisitBed) {
                homes.add("bed");
            }
            if (canVisitOthers) {
                final int sepIndex = args[0].indexOf(':');
                if (sepIndex < 0) {
                    getPlayers(user).forEach(player -> homes.add(player + ":"));
                } else {
                    final String namePart = args[0].substring(0, sepIndex);
                    final User otherUser;
                    try {
                        otherUser = getPlayer(server, new String[] {namePart}, 0, true, true);
                    } catch (final Exception ex) {
                        return homes;
                    }
                    otherUser.getHomes().forEach(home -> homes.add(namePart + ":" + home));
                    if (canVisitBed) {
                        homes.add(namePart + ":bed");
                    }
                }
            }
            return homes;
        } else {
            return Collections.emptyList();
        }
    }
}
