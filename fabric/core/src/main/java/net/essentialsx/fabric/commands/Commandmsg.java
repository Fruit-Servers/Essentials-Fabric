package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsLoopCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class Commandmsg extends EssentialsLoopCommand {
    public Commandmsg() {
        super("msg");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        String message = getFinalArg(args, 1);
        final boolean canWildcard = sender.isAuthorized("essentials.msg.multiple");
        if (sender.isPlayer()) {
            final User user = ess.getUser(sender.getPlayer());
            // Muted players are handled in SimpleMessageRecipient#sendMessage so that the message can still
            // be shown to social spies before being dropped instead of being delivered to the recipient.
            message = FormatUtil.formatMessage(user, "essentials.msg", message);
        } else {
            message = FormatUtil.replaceFormat(message);
        }
        // Sending messages to console
        if (args[0].equalsIgnoreCase(Console.NAME) || args[0].equalsIgnoreCase(Console.displayName())) {
            final IMessageRecipient messageSender = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : Console.getInstance();
            messageSender.sendMessage(Console.getInstance(), message);
            return;
        }
        loopOnlinePlayers(server, sender, false, canWildcard, args[0], new String[] {message});
    }

    @Override
    protected void updatePlayer(final MinecraftServer server, final CommandSource sender, final User messageReceiver, final String[] args) {
        final IMessageRecipient messageSender = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : Console.getInstance();
        messageSender.sendMessage(messageReceiver, args[0]); // args[0] is the message.
    }

    @Override
    protected List<String> getTabCompleteOptions(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(sender);
        } else {
            return Collections.emptyList(); // It's a chat message, send an empty list.
        }
    }
}
