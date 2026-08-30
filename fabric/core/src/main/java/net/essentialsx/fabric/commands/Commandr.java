package net.essentialsx.fabric.commands;

import net.essentialsx.fabric.command.CommandSource;
import net.essentialsx.fabric.command.Console;
import net.essentialsx.fabric.command.EssentialsCommand;
import net.essentialsx.fabric.command.NotEnoughArgumentsException;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.text.TranslatableException;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.utils.FormatUtil;
import net.minecraft.server.MinecraftServer;

public class Commandr extends EssentialsCommand {
    public Commandr() {
        super("r");
    }

    @Override
    protected void run(final MinecraftServer server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        String message = getFinalArg(args, 0);
        final IMessageRecipient messageSender;
        if (sender.isPlayer()) {
            final User user = ess.getUser(sender.getPlayer());
            // Muted players are handled in SimpleMessageRecipient#sendMessage so that the message can still
            // be shown to social spies before being dropped instead of being delivered to the recipient.
            message = FormatUtil.formatMessage(user, "essentials.msg", message);
            messageSender = user;
        } else {
            message = FormatUtil.replaceFormat(message);
            messageSender = Console.getInstance();
        }
        final IMessageRecipient target = messageSender.getReplyRecipient();
        // Check to make sure the sender does have a quick-reply recipient
        if (target == null || (!ess.getSettings().isReplyToVanished() && sender.isPlayer() && target instanceof User && ((User) target).isHiddenFrom(sender.getPlayer()))) {
            messageSender.setReplyRecipient(null);
            throw new TranslatableException("foreverAlone");
        }
        messageSender.sendMessage(target, message);
    }
}
