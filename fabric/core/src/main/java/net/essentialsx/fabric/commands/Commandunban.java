package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

public class Commandunban extends EssentialsCommand {
    public Commandunban() {
        super("unban");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        String name;
        try {
            final User user = getPlayer(server, args, 0, true, true);
            name = user.getName();
            Bans.unbanPlayer(ess, user.getUUID(), name);
        } catch (final PlayerNotFoundException e) {
            name = args[0];
            if (!Bans.isBanned(ess, null, name)) {
                throw new TranslatableException("playerNeverOnServer", args[0]);
            }
            Bans.unbanPlayer(ess, null, name);
        }
        final String senderDisplayName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.displayName();
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("playerUnbanned", senderDisplayName, name))));
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.ban.notify"), "playerUnbanned", senderDisplayName, name);
    }
}
