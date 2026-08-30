package net.essentialsx.fabric.command;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.messaging.SimpleMessageRecipient;
import net.essentialsx.fabric.text.Text;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * The server console as a message recipient.
 */
public final class Console implements IMessageRecipient {
    public static final String NAME = "Console";
    private static Console instance;
    private final Essentials ess;
    private final IMessageRecipient messageRecipient;

    private Console(final Essentials ess) {
        this.ess = ess;
        this.messageRecipient = new SimpleMessageRecipient(ess, this);
    }

    public static String displayName() {
        return tlLiteral("consoleName");
    }

    public static Console getInstance() {
        return instance;
    }

    public static void setInstance(final Essentials ess) {
        instance = new Console(ess);
    }

    public CommandSourceStack getCommandSender() {
        return ess.getServer().createCommandSourceStack();
    }

    public CommandSource getSource() {
        return new CommandSource(ess, getCommandSender());
    }

    @Override
    public String getName() {
        return Console.NAME;
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return displayName();
    }

    @Override
    public void sendMessage(final String message) {
        getCommandSender().sendSystemMessage(Text.get().legacy(message));
    }

    @Override
    public void sendTl(final String tlKey, final Object... args) {
        final String translation = tlLiteral(tlKey, args);
        if (translation.isEmpty()) {
            return;
        }
        getCommandSender().sendSystemMessage(Text.get().mini(translation));
    }

    @Override
    public String tlSender(final String tlKey, final Object... args) {
        return tlLiteral(tlKey, args);
    }

    @Override
    public boolean isReachable() {
        return true;
    }

    @Override
    public MessageResponse sendMessage(final IMessageRecipient recipient, final String message) {
        return this.messageRecipient.sendMessage(recipient, message);
    }

    @Override
    public MessageResponse onReceiveMessage(final IMessageRecipient sender, final String message) {
        return this.messageRecipient.onReceiveMessage(sender, message);
    }

    @Override
    public IMessageRecipient getReplyRecipient() {
        return this.messageRecipient.getReplyRecipient();
    }

    @Override
    public void setReplyRecipient(final IMessageRecipient recipient) {
        this.messageRecipient.setReplyRecipient(recipient);
    }

    @Override
    public boolean isHiddenFrom(final ServerPlayer player) {
        return false;
    }
}
