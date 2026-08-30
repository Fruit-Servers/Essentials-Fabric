package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandafk extends EssentialsCommand {
    public Commandafk() {
        super("afk");
    }

    @Override
    public void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && user.isAuthorized("essentials.afk.others")) {
            User afkUser = user;
            String message;
            try {
                afkUser = getPlayer(server, user, args, 0);
                message = args.length > 1 ? getFinalArg(args, 1) : null;
            } catch (final PlayerNotFoundException e) {
                message = getFinalArg(args, 0);
            }
            toggleAfk(user, afkUser, message);
        } else {
            final String message = args.length > 0 ? getFinalArg(args, 0) : null;
            toggleAfk(user, user, message);
        }
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0) {
            final User afkUser = getPlayer(server, args, 0, true, false);
            final String message = args.length > 1 ? getFinalArg(args, 1) : null;
            toggleAfk(null, afkUser, message);
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    private void toggleAfk(final User sender, final User user, final String message) throws Exception {
        if (message != null && sender != null) {
            if (sender.isMuted()) {
                final String dateDiff = sender.getMuteTimeout() > 0 ? DateUtil.formatDateDiff(sender.getMuteTimeout()) : null;
                if (dateDiff == null) {
                    if (sender.hasMuteReason()) {
                        throw new TranslatableException("voiceSilencedReason", sender.getMuteReason());
                    } else {
                        throw new TranslatableException("voiceSilenced");
                    }
                }
                if (sender.hasMuteReason()) {
                    throw new TranslatableException("voiceSilencedReasonTime", dateDiff, sender.getMuteReason());
                } else {
                    throw new TranslatableException("voiceSilencedTime", dateDiff);
                }
            }
            if (!sender.isAuthorized("essentials.afk.message")) {
                throw new TranslatableException("noPermToAFKMessage");
            }
        }
        user.setDisplayNick();
        final boolean currentStatus = user.isAfk();
        final boolean afterStatus = user.toggleAfk("COMMAND");
        if (currentStatus == afterStatus) {
            return;
        }
        String tlKey = "";
        String selfTlKey = "";
        if (!afterStatus) {
            if (!user.isHidden()) {
                tlKey = "userIsNotAway";
                selfTlKey = "userIsNotAwaySelf";
            }
            user.updateActivity(false, "COMMAND");
        } else {
            if (!user.isHidden()) {
                if (message != null) {
                    tlKey = "userIsAwayWithMessage";
                    selfTlKey = "userIsAwaySelfWithMessage";
                } else {
                    tlKey = "userIsAway";
                    selfTlKey = "userIsAwaySelf";
                }
            }
            user.setAfkMessage(message);
        }
        if (!tlKey.isEmpty() && ess.getSettings().broadcastAfkMessage()) {
            ess.broadcastTl(user, u -> u == user, tlKey, user.getDisplayName(), message);
        }
        if (!selfTlKey.isEmpty()) {
            user.sendTl(selfTlKey, user.getDisplayName(), message);
        }
        user.setDisplayNick();
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1 && sender.isAuthorized("essentials.afk.others")) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList();
        }
    }
}
