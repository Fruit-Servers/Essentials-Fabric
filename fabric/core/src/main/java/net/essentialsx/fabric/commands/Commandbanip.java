package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

public class Commandbanip extends EssentialsCommand {
    public Commandbanip() {
        super("banip");
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final String senderName = sender.isPlayer() ? sender.getDisplayName() : Console.NAME;
        final String senderDisplayName = sender.isPlayer() ? sender.getDisplayName() : Console.displayName();
        String ipAddress;
        if (FormatUtil.validIP(args[0])) {
            ipAddress = args[0];
        } else {
            try {
                final User player = getPlayer(server, args, 0, true, true);
                ipAddress = player.getIpAddress();
            } catch (final PlayerNotFoundException ex) {
                ipAddress = args[0];
            }
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            throw new PlayerNotFoundException();
        }
        final String banReason;
        if (args.length > 1) {
            banReason = FormatUtil.replaceFormat(getFinalArg(args, 1).replace("\\n", "\n").replace("|", "\n"));
        } else {
            banReason = tlLiteral("defaultBanReason");
        }
        final String banDisplay = Text.get().miniToLegacy(tlLiteral("banFormat", banReason, senderDisplayName));
        Bans.banIp(ess, ipAddress, banReason, null, senderName);
        ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("playerBanIpAddress", senderDisplayName, ipAddress, banReason)));
        for (final ServerPlayer player : new ArrayList<>(ess.getOnlinePlayers())) {
            if (ipAddress.equalsIgnoreCase(player.getIpAddress())) {
                player.connection.disconnect(Text.get().legacy(banDisplay));
            }
        }
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.banip.notify"), "playerBanIpAddress", senderDisplayName, ipAddress, banReason);
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
