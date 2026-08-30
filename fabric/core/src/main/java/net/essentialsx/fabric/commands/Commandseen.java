package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.CommonPlaceholders;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.essentialsx.fabric.utils.StringUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserBanListEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Commandseen extends EssentialsCommand {
    public Commandseen() {
        super("seen");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final boolean showBan = sender.isAuthorized("essentials.seen.banreason");
        final boolean showIp = sender.isAuthorized("essentials.seen.ip");
        final boolean showLocation = sender.isAuthorized("essentials.seen.location");
        final boolean showWhitelist = sender.isAuthorized("essentials.seen.whitelist");
        final boolean searchAccounts = commandLabel.contains("alts") && sender.isAuthorized("essentials.seen.alts");
        User player;
        // Check by uuid, if it fails check by name.
        try {
            final UUID uuid = UUID.fromString(args[0]);
            player = ess.getUser(uuid);
        } catch (final IllegalArgumentException ignored) {
            player = ess.getOfflineUser(args[0]);
        }
        if (player == null) {
            if (!searchAccounts) {
                if (sender.isAuthorized("essentials.seen.ipsearch") && FormatUtil.validIP(args[0])) {
                    if (Bans.isIpBanned(ess, args[0])) {
                        sender.sendTl("isIpBanned", args[0]);
                    }
                    seenIP(sender, args[0], args[0]);
                    return;
                } else if (Bans.isIpBanned(ess, args[0])) {
                    sender.sendTl("isIpBanned", args[0]);
                    return;
                } else if (Bans.isBanned(ess, null, args[0])) {
                    final UserBanListEntry entry = Bans.getBan(ess, null, args[0]);
                    sender.sendTl("whoisBanned", showBan && entry != null ? entry.getReason() : sender.tl("true"));
                    return;
                }
            }
            ess.runTaskAsynchronously(() -> {
                final User userFromServer = ess.getUsers().getUser(args[0]);
                try {
                    if (userFromServer != null) {
                        showSeenMessage(sender, userFromServer, searchAccounts, showBan, showIp, showLocation, showWhitelist);
                    } else {
                        try {
                            showSeenMessage(sender, getPlayer(server, sender, args, 0), searchAccounts, showBan, showIp, showLocation, showWhitelist);
                        } catch (final PlayerNotFoundException e) {
                            throw new TranslatableException("playerNeverOnServer", args[0]);
                        }
                    }
                } catch (final Exception e) {
                    ess.showError(sender, e, commandLabel);
                }
            });
        } else {
            showSeenMessage(sender, player, searchAccounts, showBan, showIp, showLocation, showWhitelist);
        }
    }

    private void showSeenMessage(final CommandSource sender, final User player, final boolean searchAccounts, final boolean showBan, final boolean showIp, final boolean showLocation, final boolean showWhitelist) {
        if (searchAccounts) {
            seenIP(sender, player.getLastLoginAddress(), player.getDisplayName());
        } else if (player.isOnline() && ess.canInteractWith(sender, player)) {
            seenOnline(sender, player, showIp);
        } else {
            seenOffline(sender, player, showBan, showIp, showLocation, showWhitelist);
        }
    }

    private void seenOnline(final CommandSource sender, final User user, final boolean showIp) {
        user.setDisplayNick();
        sender.sendTl("seenOnline", user.getDisplayName(), DateUtil.formatDateDiff(user.getLastLogin()));
        final List<String> history = user.getPastUsernames();
        if (history != null && !history.isEmpty()) {
            sender.sendTl("seenAccounts", StringUtil.joinListSkip(", ", user.getName(), history.toArray()));
        }
        if (sender.isAuthorized("essentials.seen.uuid")) {
            sender.sendTl("whoisUuid", user.getUUID().toString());
        }
        if (user.isAfk()) {
            sender.sendTl("whoisAFK", CommonPlaceholders.trueFalse(sender, true));
        }
        if (user.isJailed()) {
            sender.sendTl("whoisJail", user.getJailTimeout() > 0 ? user.getFormattedJailTime() : CommonPlaceholders.trueFalse(sender, true));
        }
        if (user.isMuted()) {
            final long muteTimeout = user.getMuteTimeout();
            if (!user.hasMuteReason()) {
                sender.sendTl("whoisMuted", muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : CommonPlaceholders.trueFalse(sender, true));
            } else {
                sender.sendTl("whoisMutedReason", muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : CommonPlaceholders.trueFalse(sender, true), user.getMuteReason());
            }
        }
        final String location = user.getGeoLocation();
        if (location != null && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.geoip.show"))) {
            sender.sendTl("whoisGeoLocation", location);
        }
        if (showIp) {
            sender.sendTl("whoisIPAddress", user.getIpAddress());
        }
    }

    private void seenOffline(final CommandSource sender, final User user, final boolean showBan, final boolean showIp, final boolean showLocation, final boolean showWhitelist) {
        user.setDisplayNick();
        if (user.getLastLogout() > 0) {
            sender.sendTl("seenOffline", user.getName(), DateUtil.formatDateDiff(user.getLastLogout()));
            final List<String> history = user.getPastUsernames();
            if (history != null && history.size() > 1) {
                sender.sendTl("seenAccounts", StringUtil.joinListSkip(", ", user.getName(), history.toArray()));
            }
            if (sender.isAuthorized("essentials.seen.uuid")) {
                sender.sendTl("whoisUuid", user.getUUID());
            }
        } else {
            sender.sendTl("userUnknown", user.getName());
        }
        if (showWhitelist) {
            final boolean whitelisted = ess.getServer().getPlayerList().getWhiteList().isWhiteListed(Bans.profile(ess, user.getUUID(), user.getName()));
            sender.sendTl("whoisWhitelist", CommonPlaceholders.trueFalse(sender, whitelisted));
        }
        if (Bans.isBanned(ess, user.getUUID(), user.getName())) {
            final UserBanListEntry banEntry = Bans.getBan(ess, user.getUUID(), user.getName());
            final Object reason = showBan && banEntry != null ? banEntry.getReason() : CommonPlaceholders.trueFalse(sender, true);
            sender.sendTl("whoisBanned", reason);
            if (banEntry != null && banEntry.getExpires() != null) {
                final Date expiry = banEntry.getExpires();
                Object expireString = Text.parsed(sender.tl("now"));
                if (expiry.after(new Date())) {
                    expireString = DateUtil.formatDateDiff(expiry.getTime());
                }
                sender.sendTl("whoisTempBanned", expireString);
            }
        }
        if (user.isMuted()) {
            final long muteTimeout = user.getMuteTimeout();
            if (!user.hasMuteReason()) {
                sender.sendTl("whoisMuted", muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : CommonPlaceholders.trueFalse(sender, true));
            } else {
                sender.sendTl("whoisMutedReason", muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : CommonPlaceholders.trueFalse(sender, true), user.getMuteReason());
            }
        }
        final String location = user.getGeoLocation();
        if (location != null && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.geoip.show"))) {
            sender.sendTl("whoisGeoLocation", location);
        }
        if (showIp) {
            if (user.getLastLoginAddress() != null && !user.getLastLoginAddress().isEmpty()) {
                sender.sendTl("whoisIPAddress", user.getLastLoginAddress());
            }
        }
        if (showLocation) {
            final LazyLocation loc = user.getLogoutLocation();
            if (loc != null) {
                sender.sendTl("whoisLocation", loc.worldDisplayName(ess.getServer()), loc.blockX(), loc.blockY(), loc.blockZ());
            }
        }
    }

    private void seenIP(final CommandSource sender, final String ipAddress, final String display) {
        sender.sendTl("runningPlayerMatch", Text.parsed(Text.get().legacyToMini(display)));
        ess.runTaskAsynchronously(() -> {
            final List<String> matches = new ArrayList<>();
            for (final UUID u : ess.getUsers().getAllUserUUIDs()) {
                final User user = ess.getUsers().getUser(u);
                if (user == null) {
                    continue;
                }
                final String uIPAddress = user.getLastLoginAddress();
                if (uIPAddress != null && !uIPAddress.isEmpty() && uIPAddress.equalsIgnoreCase(ipAddress)) {
                    matches.add(user.getName());
                }
            }
            if (matches.size() > 0) {
                sender.sendTl("matchingIPAddress");
                sender.sendTl("matchingAccounts", StringUtil.joinList(matches.toArray()));
            } else {
                sender.sendTl("noMatchingPlayers");
            }
        });
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
