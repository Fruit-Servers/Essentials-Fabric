package net.essentialsx.fabric.messaging;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Something that can send and receive private messages (players and the console).
 */
public interface IMessageRecipient {
    void sendMessage(String message);

    void sendTl(String tlKey, Object... args);

    String tlSender(String tlKey, Object... args);

    MessageResponse sendMessage(IMessageRecipient recipient, String message);

    MessageResponse onReceiveMessage(IMessageRecipient sender, String message);

    String getName();

    UUID getUUID();

    String getDisplayName();

    boolean isReachable();

    IMessageRecipient getReplyRecipient();

    void setReplyRecipient(IMessageRecipient recipient);

    boolean isHiddenFrom(ServerPlayer player);

    enum MessageResponse {
        SUCCESS,
        SUCCESS_BUT_AFK,
        MESSAGES_IGNORED,
        SENDER_IGNORED,
        UNREACHABLE,
        EVENT_CANCELLED,
        SENDER_MUTED;

        public boolean isSuccess() {
            return this == SUCCESS || this == SUCCESS_BUT_AFK;
        }
    }
}
