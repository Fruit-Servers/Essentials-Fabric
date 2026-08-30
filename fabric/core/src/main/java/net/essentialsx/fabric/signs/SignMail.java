package net.essentialsx.fabric.signs;

import net.essentialsx.fabric.Essentials;
import net.essentialsx.fabric.user.User;
import net.essentialsx.fabric.user.MailMessage;

import java.util.List;
import java.util.ListIterator;

public class SignMail extends EssentialsSign {
    public SignMail() {
        super("Mail");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final Essentials ess) throws SignException {
        final List<MailMessage> mail = player.getMailMessages();
        final ListIterator<MailMessage> iterator = mail.listIterator();
        boolean hadMail = false;
        while (iterator.hasNext()) {
            final MailMessage mailObj = iterator.next();
            if (mailObj.isExpired()) {
                iterator.remove();
                continue;
            }
            hadMail = true;
            player.sendComponent(net.essentialsx.fabric.text.Text.get().deserializeMiniMessage(ess.getMail().getMailLine(mailObj)));
            iterator.set(mailObj.asRead());
        }
        if (!hadMail) {
            player.sendTl("noNewMail");
            return false;
        }
        player.setMailList(mail);
        player.sendTl("markMailAsRead");
        return true;
    }
}
