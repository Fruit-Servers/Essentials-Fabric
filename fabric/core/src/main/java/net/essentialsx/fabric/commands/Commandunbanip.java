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
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

public class Commandunbanip extends EssentialsCommand {
    public Commandunbanip() {
        super("unbanip");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
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
        Bans.unbanIp(ess, ipAddress);
        final String senderDisplayName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.displayName();
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("playerUnbanIpAddress", senderDisplayName, ipAddress))));
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.banip.notify"), "playerUnbanIpAddress", senderDisplayName, ipAddress);
    }
}
