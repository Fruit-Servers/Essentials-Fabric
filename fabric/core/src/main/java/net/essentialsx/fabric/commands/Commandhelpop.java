package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.text.I18n;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandhelpop extends EssentialsCommand {
    public Commandhelpop() {
        super("helpop");
    }

    @Override
    protected void run(final MinecraftServer server, final User user, final String commandLabel, final String[] args) throws Exception {
        user.setDisplayNick();
        sendMessage(user, args);
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sendMessage(Console.getInstance(), args);
    }

    private void sendMessage(final IMessageRecipient from, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final String message = FormatUtil.stripFormat(getFinalArg(args, 0));
        ess.getLogger().info(FormatUtil.stripLogColorFormat(Text.get().miniToLegacy(I18n.tlLiteral("helpOp", from.getDisplayName(), message))));
        final List<IUser> recipients = new ArrayList<>();
        for (final IUser user : ess.getOnlineUsers()) {
            if (user.isAuthorized("essentials.helpop.receive")) {
                recipients.add(user);
            }
        }
        if (from instanceof IUser) {
            final IUser sender = (IUser) from;
            if (!recipients.contains(sender)) {
                from.sendTl("helpOp", from.getDisplayName(), message);
            }
        }
        for (final IUser recipient : recipients) {
            recipient.sendTl("helpOp", from.getDisplayName(), message);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        return Collections.emptyList();
    }
}
