package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.moderation.Bans;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

public class Commandban extends EssentialsCommand {
    public Commandban() {
        super("ban");
    }

    @Override
    public void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        boolean nomatch = false;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        User user;
        try {
            user = getPlayer(server, args, 0, true, true);
        } catch (final PlayerNotFoundException e) {
            nomatch = true;
            user = null;
        }
        final String targetName = user == null ? args[0] : user.getName();
        if (user == null || !user.isOnline()) {
            if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.ban.offline")) {
                throw new TranslatableException("banExemptOffline");
            }
        } else if (user.isAuthorized("essentials.ban.exempt") && sender.isPlayer()) {
            throw new TranslatableException("banExempt");
        }
        final String senderName = sender.isPlayer() ? sender.getDisplayName() : Console.NAME;
        final String senderDisplayName = sender.isPlayer() ? sender.getDisplayName() : Console.displayName();
        final String banReason;
        if (args.length > 1) {
            banReason = FormatUtil.replaceFormat(getFinalArg(args, 1).replace("\\n", "\n").replace("|", "\n"));
        } else {
            banReason = tlLiteral("defaultBanReason");
        }
        Bans.banPlayer(ess, user == null ? null : user.getUUID(), targetName, banReason, null, senderName);
        final String banDisplay = tlLiteral("banFormat", banReason, senderDisplayName);
        if (user != null && user.isOnline()) {
            ess.kickPlayer(user, Text.get().miniToLegacy(banDisplay));
        }
        ess.getLogger().info(Text.get().miniToLegacy(tlLiteral("playerBanned", senderDisplayName, targetName, banDisplay)));
        if (nomatch) {
            sender.sendTl("userUnknown", targetName);
        }
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.ban.notify"), "playerBanned", senderDisplayName, targetName, banReason);
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
