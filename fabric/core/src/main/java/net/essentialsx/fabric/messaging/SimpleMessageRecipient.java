package net.essentialsx.fabric.messaging;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.text.Text;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.User;
import net.minecraft.server.level.ServerPlayer;

import java.lang.ref.WeakReference;
import java.util.UUID;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Private message state machine shared by players and the console (Section 12.1).
 */
public class SimpleMessageRecipient implements IMessageRecipient {
    private final Essentials ess;
    private final IMessageRecipient parent;
    private long lastMessageMs;
    private WeakReference<IMessageRecipient> replyRecipient;

    public SimpleMessageRecipient(final Essentials ess, final IMessageRecipient parent) {
        this.ess = ess;
        this.parent = parent;
    }

    protected static User getUser(final IMessageRecipient recipient) {
        if (recipient instanceof SimpleMessageRecipient) {
            return ((SimpleMessageRecipient) recipient).parent instanceof User ? (User) ((SimpleMessageRecipient) recipient).parent : null;
        }
        return recipient instanceof User ? (User) recipient : null;
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(message);
    }

    @Override
    public void sendTl(final String tlKey, final Object... args) {
        this.parent.sendTl(tlKey, args);
    }

    @Override
    public String tlSender(final String tlKey, final Object... args) {
        return this.parent.tlSender(tlKey, args);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override
    public UUID getUUID() {
        return this.parent.getUUID();
    }

    @Override
    public String getDisplayName() {
        return this.parent.getDisplayName();
    }

    @Override
    public MessageResponse sendMessage(final IMessageRecipient recipient, final String message) {
        final User senderUser = getUser(this);
        if (senderUser != null && senderUser.isMuted()) {
            sendSocialSpy(recipient, message, true);
            senderUser.notifyMuted();
            return MessageResponse.SENDER_MUTED;
        }
        final MessageResponse messageResponse = recipient.onReceiveMessage(this.parent, message);
        switch (messageResponse) {
            case UNREACHABLE:
                sendTl("recentlyForeverAlone", recipient.getDisplayName());
                break;
            case MESSAGES_IGNORED:
                sendTl("msgIgnore", recipient.getDisplayName());
                break;
            case SENDER_IGNORED:
                break;
            case SUCCESS_BUT_AFK:
                if (((IUser) recipient).getAfkMessage() != null) {
                    sendTl("userAFKWithMessage", recipient.getDisplayName(), ((IUser) recipient).getAfkMessage());
                } else {
                    sendTl("userAFK", recipient.getDisplayName());
                }
            default:
                sendTl("msgFormat", Text.parsed(tlSender("meSender")), recipient.getDisplayName(), message);
                sendSocialSpy(recipient, message, false);
                break;
        }
        if (messageResponse.isSuccess()) {
            setReplyRecipient(recipient);
        }
        return messageResponse;
    }

    private void sendSocialSpy(final IMessageRecipient recipient, final String message, final boolean muted) {
        if (!ess.getSettings().isSocialSpyMessages()) {
            return;
        }
        if (muted && !ess.getSettings().getSocialSpyListenMutedPlayers()) {
            return;
        }
        final User senderUser = getUser(this);
        final User recipientUser = getUser(recipient);
        if (senderUser == null
            || senderUser.isAuthorized("essentials.chat.spy.exempt")
            || recipientUser == null
            || recipientUser.isAuthorized("essentials.chat.spy.exempt")) {
            return;
        }
        final String senderName = ess.getSettings().isSocialSpyDisplayNames() ? getDisplayName() : getName();
        final String recipientName = ess.getSettings().isSocialSpyDisplayNames() ? recipient.getDisplayName() : recipient.getName();
        final String prefix = muted ? tlSender("socialSpyMutedPrefix") : tlLiteral("socialSpyPrefix");
        for (final User onlineUser : ess.getOnlineUsers()) {
            if (onlineUser.isSocialSpyEnabled()
                && !onlineUser.equals(senderUser)
                && !onlineUser.equals(recipient)) {
                onlineUser.sendComponent(Text.get().deserializeMiniMessage(prefix + tlLiteral("socialSpyMsgFormat", senderName, recipientName, message)));
            }
        }
    }

    @Override
    public MessageResponse onReceiveMessage(final IMessageRecipient sender, final String message) {
        if (!isReachable()) {
            return MessageResponse.UNREACHABLE;
        }
        final User user = getUser(this);
        boolean afk = false;
        boolean isLastMessageReplyRecipient = ess.getSettings().isLastMessageReplyRecipient();
        if (user != null) {
            if (user.isIgnoreMsg() && sender instanceof IUser && !((IUser) sender).isAuthorized("essentials.msgtoggle.bypass")) {
                return MessageResponse.MESSAGES_IGNORED;
            }
            afk = user.isAfk();
            isLastMessageReplyRecipient = user.isLastMessageReplyRecipient();
            if (sender instanceof IUser && user.isIgnoredPlayer((IUser) sender)) {
                return MessageResponse.SENDER_IGNORED;
            }
        }
        sendTl("msgFormat", sender.getDisplayName(), Text.parsed(tlSender("meRecipient")), message);
        if (isLastMessageReplyRecipient) {
            final long timeout = ess.getSettings().getLastMessageReplyRecipientTimeout() * 1000;
            if (getReplyRecipient() == null || !getReplyRecipient().isReachable()
                || System.currentTimeMillis() - this.lastMessageMs > timeout) {
                setReplyRecipient(sender);
            }
        } else {
            setReplyRecipient(sender);
        }
        this.lastMessageMs = System.currentTimeMillis();
        return afk ? MessageResponse.SUCCESS_BUT_AFK : MessageResponse.SUCCESS;
    }

    @Override
    public boolean isReachable() {
        return this.parent.isReachable();
    }

    @Override
    public IMessageRecipient getReplyRecipient() {
        return replyRecipient == null ? null : replyRecipient.get();
    }

    @Override
    public void setReplyRecipient(final IMessageRecipient replyRecipient) {
        this.replyRecipient = new WeakReference<>(replyRecipient);
    }

    @Override
    public boolean isHiddenFrom(final ServerPlayer player) {
        return parent.isHiddenFrom(player);
    }
}
