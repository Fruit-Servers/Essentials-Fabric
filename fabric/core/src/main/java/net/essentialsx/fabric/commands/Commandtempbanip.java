package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.DateUtil;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class Commandtempbanip extends EssentialsCommand {
    public Commandtempbanip() {
        super("tempbanip");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final String senderName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.NAME;
        final String senderDisplayName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.displayName();
        String ipAddress;
        if (FormatUtil.validIP(args[0])) {
            ipAddress = args[0];
        } else {
            try {
                final User player = getPlayer(server, args, 0, true, true);
                ipAddress = player.getLastLoginAddress();
            } catch (final PlayerNotFoundException ex) {
                ipAddress = args[0];
            }
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            throw new PlayerNotFoundException();
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
        Bans.banIp(ess, ipAddress, banReason, new Date(banTimestamp), senderName);
        final String banDisplay = Text.get().miniToLegacy(I18n.tlLiteral("banFormat", banReason, senderDisplayName));
        for (final ServerPlayer player : new ArrayList<>(ess.getOnlinePlayers())) {
            if (ipAddress.equalsIgnoreCase(player.getIpAddress())) {
                ess.kickPlayer(ess.getUser(player), banDisplay);
            }
        }
        final String tlKey = "playerTempBanIpAddress";
        final Object[] objects = {senderDisplayName, ipAddress, banReason, DateUtil.formatDateDiff(banTimestamp), banReason};
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral(tlKey, objects))));
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.banip.notify"), tlKey, objects);
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
