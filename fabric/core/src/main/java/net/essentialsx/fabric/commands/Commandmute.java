package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NoChargeException;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class Commandmute extends EssentialsCommand {
    public Commandmute() {
        super("mute");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final boolean isUnmute = commandLabel.toLowerCase(Locale.ENGLISH).contains("unmute");
        boolean nomatch = false;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        User user;
        try {
            user = getPlayer(server, args, 0, true, true);
        } catch (final PlayerNotFoundException e) {
            nomatch = true;
            user = ess.getOfflineUser(args[0]);
            if (user == null) {
                throw e;
            }
        }
        if (isUnmute && !user.getMuted()) {
            sender.sendTl("playerNotMuted", user.getDisplayName());
            return;
        }
        if (!user.isOnline() && sender.isPlayer()) {
            if (!sender.isAuthorized("essentials.mute.offline")) {
                throw new TranslatableException("muteExemptOffline");
            }
        } else if (!isUnmute && user.isAuthorized("essentials.mute.exempt")) {
            throw new TranslatableException("muteExempt");
        }
        long muteTimestamp = 0;
        final String time;
        String muteReason = null;
        if (!isUnmute && args.length > 1) {
            time = args[1];
            try {
                muteTimestamp = DateUtil.parseDateDiff(time, true);
                muteReason = getFinalArg(args, 2);
            } catch (final Exception e) {
                muteReason = getFinalArg(args, 1);
            }
            final long maxMuteLength = ess.getSettings().getMaxMute() * 1000;
            if (maxMuteLength > 0 && ((muteTimestamp - GregorianCalendar.getInstance().getTimeInMillis()) > maxMuteLength) && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.mute.unlimited")) {
                sender.sendTl("oversizedMute");
                throw new NoChargeException();
            }
        }
        if (muteReason != null) {
            user.setMuteReason(muteReason.isEmpty() ? null : muteReason);
            user.setMuted(true);
        } else {
            user.setMuted(!user.getMuted());
            if (!user.getMuted()) {
                user.setMuteReason(null);
            }
        }
        user.setMuteTimeout(muteTimestamp);
        final boolean muted = user.getMuted();
        final String muteTime = DateUtil.formatDateDiff(muteTimestamp);
        if (nomatch) {
            sender.sendTl("userUnknown", user.getName());
        }
        final String senderName = sender.getName();
        if (muted) {
            if (muteTimestamp > 0) {
                if (!user.hasMuteReason()) {
                    sender.sendTl("mutedPlayerFor", user.getDisplayName(), muteTime);
                    user.sendTl("playerMutedFor", muteTime);
                } else {
                    sender.sendTl("mutedPlayerForReason", user.getDisplayName(), muteTime, user.getMuteReason());
                    user.sendTl("playerMutedForReason", muteTime, user.getMuteReason());
                }
            } else {
                if (!user.hasMuteReason()) {
                    sender.sendTl("mutedPlayer", user.getDisplayName());
                    user.sendTl("playerMuted");
                } else {
                    sender.sendTl("mutedPlayerReason", user.getDisplayName(), user.getMuteReason());
                    user.sendTl("playerMutedReason", user.getMuteReason());
                }
            }
            final String tlKey;
            final Object[] objects;
            if (user.hasMuteReason()) {
                if (muteTimestamp > 0) {
                    tlKey = "muteNotifyForReason";
                    objects = new Object[] {senderName, user.getName(), muteTime, user.getMuteReason()};
                } else {
                    tlKey = "muteNotify";
                    objects = new Object[] {senderName, user.getName(), user.getMuteReason()};
                }
            } else {
                tlKey = muteTimestamp > 0 ? "muteNotifyFor" : "muteNotify";
                objects = new Object[] {senderName, user.getName(), muteTime};
            }
            ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral(tlKey, objects))));
            ess.broadcastTl(null, u -> !u.isAuthorized("essentials.mute.notify"), tlKey, objects);
        } else {
            sender.sendTl("unmutedPlayer", user.getDisplayName());
            user.sendTl("playerUnmuted");
            ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("unmuteNotify", senderName, user.getName()))));
            ess.broadcastTl(null, u -> !u.isAuthorized("essentials.mute.notify"), "unmuteNotify", senderName, user.getName());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return COMMON_DATE_DIFFS; // Date diff can span multiple words
        }
    }
}
