package net.essentialsx.fabric.mail;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.messaging.IMessageRecipient;
import net.essentialsx.fabric.user.IUser;
import net.essentialsx.fabric.user.MailMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

/**
 * Persistent mail (Section 12.1).
 */
public class MailService {
    private final transient ThreadLocal<SimpleDateFormat> df = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy/MM/dd HH:mm"));
    private final Essentials ess;

    public MailService(final Essentials ess) {
        this.ess = ess;
    }

    public void sendMail(final IUser recipient, final IMessageRecipient sender, final String message) {
        sendMail(recipient, sender, message, 0L);
    }

    public void sendMail(final IUser recipient, final IMessageRecipient sender, final String message, final long expireAt) {
        sendMail(recipient, new MailMessage(false, false, sender.getName(), sender.getUUID(), System.currentTimeMillis(), expireAt, message));
    }

    public void sendLegacyMail(final IUser recipient, final String message) {
        sendMail(recipient, new MailMessage(false, true, null, null, 0L, 0L, message));
    }

    private void sendMail(final IUser recipient, final MailMessage message) {
        final List<MailMessage> messages = recipient.getMailMessages();
        messages.add(0, message);
        recipient.setMailList(messages);
    }

    public String getMailLine(final MailMessage mail) {
        final String message = mail.getMessage();
        if (mail.isLegacy()) {
            return tlLiteral("mailMessage", message);
        }
        final String expire = mail.getTimeExpire() != 0 ? "Timed" : "";
        return tlLiteral((mail.isRead() ? "mailFormatNewRead" : "mailFormatNew") + expire, df.get().format(new Date(mail.getTimeSent())), mail.getSenderUsername(), message);
    }

    public String getMailTlKey(final MailMessage message) {
        if (message.isLegacy()) {
            return "mailMessage";
        }
        final String expire = message.getTimeExpire() != 0 ? "Timed" : "";
        return (message.isRead() ? "mailFormatNewRead" : "mailFormatNew") + expire;
    }

    public Object[] getMailTlArgs(final MailMessage message) {
        if (message.isLegacy()) {
            return new Object[] {message.getMessage()};
        }
        return new Object[] {df.get().format(new Date(message.getTimeSent())), message.getSenderUsername(), message.getMessage()};
    }
}
