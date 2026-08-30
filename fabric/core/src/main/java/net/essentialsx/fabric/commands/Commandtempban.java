package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class Commandtempban extends EssentialsCommand {
    public Commandtempban() {
        super("tempban");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, args, 0, true, true);
        if (!user.isOnline() && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.tempban.offline")) {
            sender.sendTl("tempbanExemptOffline");
            return;
        } else if (user.isAuthorized("essentials.tempban.exempt") && sender.isPlayer()) {
            sender.sendTl("tempbanExempt");
            return;
        }
        final String time = getFinalArg(args, 1);
        final long banTimestamp = DateUtil.parseDateDiff(time, true);
        String banReason = FormatUtil.replaceFormat(DateUtil.removeTimePattern(time));
        final long maxBanLength = ess.getSettings().getMaxTempban() * 1000;
        if (maxBanLength > 0 && ((banTimestamp - GregorianCalendar.getInstance().getTimeInMillis()) > maxBanLength) && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.tempban.unlimited")) {
            sender.sendTl("oversizedTempban");
            return;
        }
        if (banReason.length() < 2) {
            banReason = I18n.tlLiteral("defaultBanReason");
        }
        final String senderName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.NAME;
        final String senderDisplayName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.displayName();
        Bans.banPlayer(ess, user.getUUID(), user.getName(), banReason, new Date(banTimestamp), senderName);
        final String expiry = DateUtil.formatDateDiff(banTimestamp);
        final String banDisplay = user.playerTl("tempBanned", expiry, senderDisplayName, banReason);
        if (user.isOnline()) {
            ess.kickPlayer(user, Text.get().miniToLegacy(banDisplay));
        }
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("playerTempBanned", senderDisplayName, user.getName(), expiry, banReason))));
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.ban.notify"), "playerTempBanned", senderDisplayName, user.getName(), expiry, banReason);
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return COMMON_DATE_DIFFS;
        }
    }
}
