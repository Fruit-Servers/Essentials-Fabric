package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.command.PlayerNotFoundException;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandkick extends EssentialsCommand {
    public Commandkick() {
        super("kick");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User target = getPlayer(server, args, 0, true, false);
        final User user = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        if (user != null) {
            if (target.isHidden(sender.getPlayer()) && !user.canInteractVanished() && target.isHiddenFrom(sender.getPlayer())) {
                throw new PlayerNotFoundException();
            }
            if (target.isAuthorized("essentials.kick.exempt")) {
                throw new TranslatableException("kickExempt");
            }
        }
        String kickReason = args.length > 1 ? getFinalArg(args, 1) : Text.get().miniToLegacy(I18n.tlLiteral("kickDefault"));
        kickReason = FormatUtil.replaceFormat(kickReason.replace("\\n", "\n").replace("|", "\n"));
        ess.kickPlayer(target, kickReason);
        final String senderDisplayName = sender.isPlayer() ? ess.getUser(sender.getPlayer()).getDisplayName() : Console.displayName();
        final String tlKey = "playerKicked";
        final Object[] objects = {senderDisplayName, target.getName(), kickReason};
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral(tlKey, objects))));
        ess.broadcastTl(null, u -> !u.isAuthorized("essentials.kick.notify"), tlKey, objects);
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
