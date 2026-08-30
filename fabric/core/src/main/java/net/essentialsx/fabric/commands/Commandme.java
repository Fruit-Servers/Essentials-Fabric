package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.LazyLocation;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Commandme extends EssentialsCommand {
    public Commandme() {
        super("me");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (user.isMuted()) {
            final String dateDiff = user.getMuteTimeout() > 0 ? DateUtil.formatDateDiff(user.getMuteTimeout()) : null;
            if (dateDiff == null) {
                throw new TranslatableException(user.hasMuteReason() ? "voiceSilencedReason" : "voiceSilenced", user.getMuteReason());
            }
            throw new TranslatableException(user.hasMuteReason() ? "voiceSilencedReasonTime" : "voiceSilencedTime", dateDiff, user.getMuteReason());
        }
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        String message = getFinalArg(args, 0);
        message = FormatUtil.formatMessage(user, "essentials.chat", message);
        user.setDisplayNick();
        long radius = ess.getSettings().getChatRadius();
        if (radius < 1) {
            ess.broadcastTl("action", user.getDisplayName(), message);
            return;
        }
        radius *= radius;
        final LazyLocation loc = user.getLocation();
        final Set<User> outList = new HashSet<>();
        for (final User onlineUser : ess.getOnlineUsers()) {
            if (!onlineUser.equals(user)) {
                boolean abort = false;
                final LazyLocation playerLoc = onlineUser.getLocation();
                if (!playerLoc.sameWorld(loc)) {
                    abort = true;
                } else if (onlineUser.isIgnoredPlayer(user)) {
                    abort = true;
                } else {
                    final double delta = playerLoc.distanceSquared(loc);
                    if (delta > radius) {
                        abort = true;
                    }
                }
                if (abort) {
                    if (onlineUser.isAuthorized("essentials.chat.spy")) {
                        outList.add(onlineUser);
                    }
                } else {
                    outList.add(onlineUser);
                }
            } else {
                outList.add(onlineUser);
            }
        }
        if (outList.size() < 2) {
            user.sendTl("localNoOne");
        }
        for (final User onlineUser : outList) {
            onlineUser.sendTl("action", user.getDisplayName(), message);
        }
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        String message = getFinalArg(args, 0);
        message = FormatUtil.replaceFormat(message);
        ess.broadcastTl("action", "@", message);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        return Collections.emptyList();
    }
}
